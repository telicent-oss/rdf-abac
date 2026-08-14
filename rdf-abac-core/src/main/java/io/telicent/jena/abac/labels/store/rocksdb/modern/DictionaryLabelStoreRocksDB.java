package io.telicent.jena.abac.labels.store.rocksdb.modern;

import io.telicent.jena.abac.labels.*;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
import io.telicent.smart.cache.storage.RestoreConfig;
import io.telicent.smart.cache.storage.RestoreException;
import io.telicent.smart.cache.storage.RestoreStatus;
import io.telicent.smart.cache.storage.labels.rocksdb.RocksDbLabelsStore;
import io.telicent.smart.cache.storage.rocksdb.KeyValue;
import io.telicent.smart.cache.storage.rocksdb.TransactionContext;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.JenaTransactionException;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.rocksdb.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;

/**
 * A labels store backed by RocksDB where the labels are dictionary encoded to reduce storage consumption
 * <p>
 * This basically wraps the {@link RocksDbLabelsStore} implementation from Smart Cache Storage libraries into the
 * RDF-ABAC {@link LabelsStore} API.
 * </p>
 * <p>
 * This may be used to open a RocksDB database previously created using the {@link LegacyLabelsStoreRocksDB}, if that
 * occurs then automated data migration from the old store format to the new store format will be attempted.  If this
 * fails then the constructor will throw an error, and you will be unable to open the location.  Only legacy stores
 * created using either {@link StoreFmtByString} or {@link StoreFmtByHash} are supported for migration, the legacy store
 * format is automatically detected.  Unfortunately legacy stores did not record which hash function they were using so
 * if you have a legacy store you <strong>MUST</strong> ensure that you specify the same hash function when opening it
 * otherwise the migration will migrate keys using the wrong hash function and none of your labels will be correctly
 * retrieved post migration.
 * </p>
 */
@SuppressWarnings({ "deprecation", "java:S1168", "java:S135", "java:S2386", "java:S1135", "java:S5164" })
public class DictionaryLabelStoreRocksDB extends RocksDbLabelsStore implements LabelsStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryLabelStoreRocksDB.class);

    public static final byte[] TRUE_BYTES = "true".getBytes(StandardCharsets.UTF_8);

    /**
     * Thread local byte buffers for encoding keys.  The size of this buffer is based upon the maximum hash length
     * (since we only allow {@link StoreFmtByHash} to be used) times 4. This is because we're mapping {@link Quad}'s to
     * labels, and each of the 4 nodes that constitute the quad is hashed separately to form the key into the label
     * store.
     */
    private final ThreadLocal<ByteBuffer> keyBuffer =
            ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(4 * HasherUtil.MAX_HASH_LENGTH).order(
                    ByteOrder.LITTLE_ENDIAN));
    private final StoreFmt storeFmt;
    private final StoreFmt.Encoder encoder;
    @SuppressWarnings("unused")
    private final StoreFmt.Parser parser;
    private final JenaTransactionWrapper wrapper;
    private static final int LABEL_LOOKUP_CACHE_SIZE = 1_000_000;
    // Hit cache of triple to list of strings (labels).
    private final Cache<Quad, Label> labelCache = CacheFactory.createCache(LABEL_LOOKUP_CACHE_SIZE);

    private final ReentrantReadWriteLock storeLock = new ReentrantReadWriteLock();

    /**
     * Creates a new dictionary encoded labels store backed by RocksDB
     *
     * @param dbPath   Database directory
     * @param storeFmt Store Format
     * @throws IllegalArgumentException Thrown if an unsupported store format is provided
     * @throws IOException              Thrown if there's a problem accessing the database directory
     * @throws RocksDBException         Thrown if there's a problem accessing the RocksDB database in the given
     *                                  directory
     */
    public DictionaryLabelStoreRocksDB(File dbPath, StoreFmt storeFmt) throws IOException, RocksDBException {
        super(dbPath);

        this.storeFmt = Objects.requireNonNull(storeFmt);
        if (!(this.storeFmt instanceof StoreFmtByHash)) {
            throw new IllegalArgumentException("Only StoreFmtByHash is supported");
        }
        this.encoder = this.storeFmt.createEncoder();
        this.parser = this.storeFmt.createParser();
        this.wrapper = new JenaTransactionWrapper(this);

        performMigrations(dbPath);
        validateStoreFormat(dbPath, storeFmt);
    }

    /**
     * Formats a counter in human-readable fashion i.e. with the thousand separator present
     *
     * @param counter Counter whose current value should be formatted
     * @return Human-readable count as a string
     */
    private static String humanReadableCount(AtomicLong counter) {
        return String.format("%,d", counter.get());
    }

    private static String humanReadableCount(long count) {
        return String.format("%,d", count);
    }

    /**
     * Calculates and formats a percentage in human-readable format
     *
     * @param current Current value of a counter
     * @param total   Total count of things being processed
     * @return A human-readable percentage formatted with 2 significant figures
     */
    private static String percentage(long current, long total) {
        if (current == total) {
            return "100%";
        } else {
            double percentage = (double) current / (double) total;
            return String.format("%.2f", percentage * 100) + "%";
        }
    }

    /**
     * Validates that the store format recorded in this database matches that with which we have been asked to open it
     * <p>
     * If this is the first time this database has been opened record the format now for future reference.
     * </p>
     *
     * @param dbPath   Database path
     * @param storeFmt Store Format
     * @throws RocksDBException Thrown if there's a problem reading/writing the store format
     */
    private void validateStoreFormat(File dbPath, StoreFmt storeFmt) throws RocksDBException {
        // Validate Store Format matches
        try (TransactionContext context = this.begin()) {
            byte[] storeFormat = context.get(this.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY);
            if (storeFormat == null) {
                // First time opening this store so record the configured store format for future reference
                context.put(this.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY, storeFmt.toString().getBytes(
                        StandardCharsets.UTF_8));
                context.commit();
            } else {
                // Opening a pre-existing store so verify the recorded format matches our configured format
                verifyStoreFormat(dbPath, storeFormat);
            }
        }
    }

    /**
     * Performs any database schema migrations required
     * <p>
     * Currently this just supports migration from the legacy format used by {@link LegacyLabelsStoreRocksDB} to this
     * format, see {@link LegacyToDictionaryMigrator} for that implementation.
     * </p>
     *
     * @param dbPath Database path
     * @throws RocksDBException Thrown if there is a problem migrating data
     */
    private void performMigrations(File dbPath) throws RocksDBException {
        // Detect whether we've been asked to open a legacy store
        boolean migrationNeeded = false;
        try (TransactionContext context = this.begin()) {
            if (!context.isEmpty(this.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO))) {
                LOGGER.info(
                        "RocksDB store at {} contains data in a legacy format, checking whether automatic migration is needed...",
                        dbPath.getAbsolutePath());

                // Check the legacyMigration key, if this is set to the TRUE Bytes then migration has happened
                // previously and need not happen again
                // If it's not set, or set to some other value, then we're partway through a migration which we will
                // resume
                byte[] migrated =
                        context.get(this.getDefaultHandle(), LegacyToDictionaryMigrator.LEGACY_MIGRATION_KEY);
                if (!Arrays.equals(migrated, TRUE_BYTES)) {
                    migrationNeeded = true;
                }
            }
        }
        if (migrationNeeded) {
            LegacyToDictionaryMigrator migrator = new LegacyToDictionaryMigrator(this);
            migrator.migrateLegacyStorage(dbPath);
        }
    }

    /**
     * Verifies that the recorded store format matches the configured store format, if not throw an error
     *
     * @param dbPath         Database path
     * @param recordedFormat Recorded store format
     * @throws IllegalStateException Thrown if the recorded and configured store formats are not matching
     */
    private void verifyStoreFormat(File dbPath, byte[] recordedFormat) {
        if (!Arrays.equals(recordedFormat, this.storeFmt.toString().getBytes(StandardCharsets.UTF_8))) {
            throw new IllegalStateException(
                    "The RocksDB store at " + dbPath + " was previously created with Store Format " + new String(
                            recordedFormat,
                            StandardCharsets.UTF_8) + " but was requested to open with different Store Format " + storeFmt + " which will lead to incorrect operation, refusing to start");
        }
    }

    @Override
    protected Options createDefaultOptions() {
        // TODO Once SC-Storage 0.11.0 is available these settings are applied in our base class and can be removed
        Options options = super.createDefaultOptions();
        RocksDBHelper.configureRocksOptions(options);
        return options;
    }

    @Override
    protected ColumnFamilyOptions defaultColumnFamilyOptions() {
        // TODO Once SC-Storage 0.11.0 is available these settings are applied in our base class and can be removed
        return super.defaultColumnFamilyOptions()
                    .setLevelCompactionDynamicLevelBytes(true)
                    .setCompressionType(CompressionType.LZ4_COMPRESSION)
                    .setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION);
    }

    @Override
    protected List<ColumnFamilyDescriptor> prepareColumnFamilyDescriptors(ColumnFamilyOptions cfOptions) {
        // Set of current column families comes from the base implementation in Smart Cache Storage
        List<ColumnFamilyDescriptor> descriptors = new ArrayList<>(super.prepareColumnFamilyDescriptors(cfOptions));

        // Add the legacy column families, if the SPO family contains data then we will automatically migrate it the
        // first time we're asked to read a legacy store with this column family non-empty
        // We have to declare these column families even if the location might not be a legacy store otherwise we
        // cannot safely open a legacy store
        for (byte[] name : RocksDBHelper.LEGACY_COLUMN_FAMILIES) {
            descriptors.add(new ColumnFamilyDescriptor(name, cfOptions));
        }
        return descriptors;
    }


    @Override
    public Label labelForQuad(Quad quad) {
        Label label = labelCache.get(quad, this::labelForQuadInternal);
        // NB - Label.EMPTY is used as a placeholder value so we hold database misses in the cache, otherwise every
        //      missed lookup would bypass the cache (as the cache does not store null) and require a full database
        //      lookup which is bad for performance
        return label == Label.EMPTY ? null : label;
    }

    private void verifyWritableTransaction() {
        if (this.wrapper.isInTransaction() && !this.wrapper.isWriteLikeTransaction()) {
            throw new JenaTransactionException("Cannot write in a read-only transaction");
        }
    }

    protected Label labelForQuadInternal(Quad quad) {
        quad = RocksDBHelper.normalize(quad);
        if (!quad.isConcrete()) {
            throw new LabelsException(
                    "Asked for labels for a quad with wildcards: " + NodeFmtLib.strNodesTTL(quad.getGraph(),
                                                                                            quad.getSubject(),
                                                                                            quad.getPredicate(),
                                                                                            quad.getObject()));
        }
        ByteBuffer buffer = keyBuffer.get().clear();
        this.encoder.formatQuad(buffer, quad.getGraph(), quad.getSubject(), quad.getPredicate(), quad.getObject());
        buffer.flip();

        byte[] key = asByteArray(buffer);
        byte[] label = this.getLabelAsBytes(key);
        return label != null ? new Label(label, StandardCharsets.UTF_8) : Label.EMPTY;
    }

    /**
     * Converts a {@link ByteBuffer} to a {@code byte[]}
     * <p>
     * This method assumes the caller has placed the buffer into read mode by calling {@link ByteBuffer#flip()} and that
     * the entire buffer should be read as a byte array.
     * </p>
     *
     * @param buffer Byte buffer
     * @return Byte Array
     */
    static byte[] asByteArray(ByteBuffer buffer) {
        byte[] key = new byte[buffer.limit()];
        buffer.get(key);
        return key;
    }

    @Override
    public Transactional getTransactional() {
        return this.wrapper;
    }

    @Override
    public void add(Quad quad, Label label) {
        verifyWritableTransaction();
        quad = RocksDBHelper.normalize(quad);

        if (!quad.isConcrete()) {
            throw new LabelsException(
                    "Tried to set labels for a quad with wildcards: " + NodeFmtLib.strNodesTTL(quad.getGraph(),
                                                                                               quad.getSubject(),
                                                                                               quad.getPredicate(),
                                                                                               quad.getObject()));
        }
        ByteBuffer buffer = keyBuffer.get().clear();
        this.encoder.formatQuad(buffer, quad.getGraph(), quad.getSubject(), quad.getPredicate(), quad.getObject());
        buffer.flip();
        byte[] key = asByteArray(buffer);

        // Store the label and associate the label with this quad as a single atomic transaction
        // Calling beginNested() ensures that when the called methods call begin() they share the same transaction
        // rather than performing their actions in independent transactions
        try (TransactionContext context = this.beginNested()) {
            long labelId = this.idForLabel(label.getData());
            this.setLabel(key, labelId);

            context.commit();
        } catch (RocksDBException e) {
            throw new LabelsException("Failed to store label in RocksDB", e);
        }

        // Update the cache when we successfully update
        labelCache.put(quad, label);
    }

    @Override
    public void remove(Quad quad) {
        final Quad normalizedQuad = RocksDBHelper.normalize(quad);
        final Node graph = normalizedQuad.getGraph();
        final Node subject = normalizedQuad.getSubject();
        final Node predicate = normalizedQuad.getPredicate();
        final Node object = normalizedQuad.getObject();

        if (!normalizedQuad.isConcrete()) {
            throw new LabelsException(
                    "Tried to remove labels for a quad with wildcards: " +
                            NodeFmtLib.strNodesTTL(graph, subject, predicate, object));
        }
        final ByteBuffer buffer = keyBuffer.get().clear();
        this.encoder.formatQuad(buffer, graph, subject, predicate, object);
        buffer.flip();
        final byte[] key = asByteArray(buffer);

        try (TransactionContext context = this.begin()) {
            context.delete(this.getHandle(KEYS_TO_LABELS_CF), key);
            context.commit();
        } catch (RocksDBException e) {
            throw new LabelsException("Failed to remove label from RocksDB", e);
        }

        labelCache.remove(normalizedQuad);
    }

    @Override
    public boolean isEmpty() {
        try (TransactionContext context = this.beginReadOnly()) {
            return context.isEmpty(this.getHandle(KEYS_TO_LABELS_CF));
        }
    }

    @Override
    public void forEach(BiConsumer<Quad, Label> action) {
        throw new UnsupportedOperationException("Original quads are not stored so cannot be iterated over");
    }

    @Override
    public Graph asGraph() {
        return null;
    }

    @Override
    public Map<String, String> getProperties() {
        return Map.of("size", Long.toString(this.keyCount()));
    }

    @Override
    public RestoreStatus restore(RestoreConfig config) throws RestoreException {
        storeLock.writeLock().lock();
        try {
            RestoreStatus status = super.restore(config);
            // Upon successful restore clear the labels cache otherwise we could return outdated labels for quads whose
            // labels have previously been cached
            if (status.isSuccess()) {
                this.labelCache.clear();
            }
            return status;
        } finally {
            storeLock.writeLock().unlock();
        }
    }

    /**
     * A helper wrapper that exposes Jena's {@link Transactional} API as required by the RDF ABAC {@link LabelsStore}
     * API backed by the internal {@link TransactionContext} of our RocksDB storage module.
     * <p>
     * This is a thread-safe singleton (since transactions in Jena are thread scoped) using a {@link ThreadLocal} to
     * hold the underlying RocksDB transaction.
     * </p>
     */
    private static final class JenaTransactionWrapper implements Transactional {

        private final DictionaryLabelStoreRocksDB store;
        private final ThreadLocal<TransactionContext> context;
        private final ThreadLocal<TxnType> requestedTxnType;
        private final ThreadLocal<Boolean> promotedToWrite;

        /**
         * Creates a new transaction wrapper
         *
         * @param store Store
         */
        JenaTransactionWrapper(DictionaryLabelStoreRocksDB store) {
            this.store = store;
            this.context = ThreadLocal.withInitial(() -> null);
            this.requestedTxnType = ThreadLocal.withInitial(() -> null);
            this.promotedToWrite = ThreadLocal.withInitial(() -> Boolean.FALSE);
        }

        @Override
        public void begin(TxnType type) {
            verifyNoTransaction();
            beginInternal(type != null ? type : TxnType.WRITE);
        }

        @Override
        public void begin(ReadWrite readWrite) {
            verifyNoTransaction();
            beginInternal(readWrite == ReadWrite.READ ? TxnType.READ : TxnType.WRITE);
        }

        @Override
        public void begin() {
            verifyNoTransaction();
            beginInternal(TxnType.WRITE);
        }

        private void beginInternal(TxnType type) {
            this.context.set(requiresWriteContext(type) ? this.store.beginNested() : this.store.beginReadOnly());
            this.requestedTxnType.set(type);
            this.promotedToWrite.set(Boolean.FALSE);
        }

        private boolean requiresWriteContext(TxnType type) {
            return type == TxnType.WRITE;
        }

        private void verifyNoTransaction() {
            if (this.isInTransaction()) {
                throw new JenaTransactionException("Already in a transaction");
            }
        }

        private void verifyTransaction() {
            if (!this.isInTransaction()) {
                throw new JenaTransactionException("Not in a transaction");
            }
        }

        @Override
        public boolean promote(Promote mode) {
            verifyTransaction();
            if (isWriteLikeTransaction()) {
                return true;
            }

            TxnType type = this.requestedTxnType.get();
            if (type != TxnType.READ && type != TxnType.READ_PROMOTE && type != TxnType.READ_COMMITTED_PROMOTE) {
                return false;
            }

            TransactionContext current = this.context.get();
            if (current != null) {
                current.close();
            }
            this.context.set(this.store.beginNested());
            this.promotedToWrite.set(Boolean.TRUE);
            return true;
        }

        @Override
        public void commit() {
            verifyTransaction();
            try {
                this.context.get().commit();
                cleanupTransactionContext(false);
            } catch (RocksDBException e) {
                throw new JenaTransactionException(e);
            }
        }

        @Override
        public void abort() {
            verifyTransaction();
            cleanupTransactionContext(true);
        }

        @Override
        public void end() {
            // The Jena contract (at least when using its Txn helper) is that end() always gets called even after a
            // commit()/abort()
            TransactionContext current = this.context.get();
            if (current == null) {
                return;
            }
            if (current.isActive()) {
                // If a transaction ends without a commit we need to treat this as an abort and clear the label
                // cache otherwise changes will leak beyond the aborted transaction
                cleanupTransactionContext(true);
            } else {
                clearThreadLocals();
            }
        }

        private boolean isWriteLikeTransaction() {
            TxnType requestedType = this.requestedTxnType.get();
            return requestedType == TxnType.WRITE || Boolean.TRUE.equals(this.promotedToWrite.get());
        }

        /**
         * Ensure the nested transaction context is fully closed so RocksDB read/write options do not linger until a
         * later end() call.
         */
        private void cleanupTransactionContext(boolean clearCache) {
            TransactionContext current = this.context.get();
            try {
                if (clearCache) {
                    store.labelCache.clear();
                }
                if (current != null) {
                    current.close();
                }
            } finally {
                clearThreadLocals();
            }
        }

        private void clearThreadLocals() {
            this.context.remove();
            this.requestedTxnType.remove();
            this.promotedToWrite.remove();
        }

        @Override
        public ReadWrite transactionMode() {
            return isInTransaction() ? (isWriteLikeTransaction() ? ReadWrite.WRITE : ReadWrite.READ) : null;
        }

        @Override
        public TxnType transactionType() {
            return isInTransaction() ? this.requestedTxnType.get() : null;
        }

        @Override
        public boolean isInTransaction() {
            return this.context.get() != null && this.context.get().isActive();
        }
    }

    /**
     * Encapsulates all the necessary logic for migrating from the on-disk format used by
     * {@link LegacyLabelsStoreRocksDB} to the format used by this implementation
     */
    @SuppressWarnings("deprecation")
    private static final class LegacyToDictionaryMigrator {
        public static final byte[] LEGACY_MIGRATION_KEY = "legacyMigration".getBytes(StandardCharsets.UTF_8);
        public static final byte[] LEGACY_MIGRATION_TARGET = "legacyMigrationTarget".getBytes(StandardCharsets.UTF_8);
        public static final byte[] LEGACY_MIGRATION_COUNTER = "legacyMigrationCounter".getBytes(StandardCharsets.UTF_8);
        public static final byte[] LEGACY_MIGRATION_CORRUPTED_COUNTER =
                "legacyMigrationCorruptedCounter".getBytes(StandardCharsets.UTF_8);

        /**
         * The migration batch size, i.e. how many keys do we migrate in a single RocksDB transaction.  This is a
         * balance between frequency of commits (to ensure durability of the migration) and the memory overheads of a
         * transaction.  Experimentation has shown 1 million keys to be a reasonable number which achieves a migration
         * throughput of roughly 3.5 million keys per minute on a representative production label store.
         */
        public static final int MIGRATION_BATCH_SIZE = 1_000_000;
        /**
         * Acceptable corruption threshold (currently 0.1 aka 10%) above which legacy store migrations will fail.  If
         * there are only a few corrupt keys in the legacy store (which can happen as the legacy store isn't using
         * RocksDB in a transaction safe manner) then we just ignore these and migrate the valid keys.
         */
        public static final double LEGACY_MIGRATION_ACCEPTABLE_CORRUPTION_THRESHOLD = 0.1;
        
        private final DictionaryLabelStoreRocksDB store;
        private final byte[] defaultGraphBytes;

        /**
         * Creates a new migrator
         *
         * @param store Store we're migrating to
         */
        LegacyToDictionaryMigrator(DictionaryLabelStoreRocksDB store) {
            this.store = store;

            // If we're migrating from a hash format store we'll be prepending the key with our default graph hash which
            // we can compute just once for performance
            ByteBuffer buffer = store.keyBuffer.get().clear();
            store.encoder.formatSingleNode(buffer, Quad.defaultGraphIRI);
            this.defaultGraphBytes = asByteArray(buffer.flip());
        }

        private record MigrationState(StoreFmt sourceFormat, StoreFmt.Parser parser, AtomicLong counter,
                                      AtomicLong corrupted, ByteBuffer migrationBuffer, long keysToMigrate) {
        }

        /**
         * Migrates data from the legacy store format to the current format
         *
         * @param dbPath Database path
         * @throws RocksDBException Thrown if there is a problem performing RocksDB operations
         */
        @SuppressWarnings("deprecation")
        private void migrateLegacyStorage(File dbPath) throws RocksDBException {
            LOGGER.info("Beginning legacy format migration...");
            try {
                MigrationState state = initialiseMigrationState(dbPath);
                migrateLegacyBatches(state);
                logMigrationSummary(state);
                validateCorruptionThreshold(dbPath, state);
                completeLegacyMigration();
            } catch (Throwable e) {
                handleLegacyMigrationFailure(dbPath, e);
            }
        }

        private MigrationState initialiseMigrationState(File dbPath) throws RocksDBException {
            StoreFmt sourceFormat = detectLegacyStorageFormat(dbPath);
            StoreFmt.Parser parser = sourceFormat.createParser();
            AtomicLong counter = new AtomicLong(0);
            AtomicLong corrupted = new AtomicLong(0);
            ByteBuffer migrationBuffer = ByteBuffer.allocate(LegacyLabelsStoreRocksDB.DEFAULT_BUFFER_CAPACITY * 10)
                                                   .order(ByteOrder.LITTLE_ENDIAN);
            long keysToMigrate = initialiseMigrationCounters(counter, corrupted);
            return new MigrationState(sourceFormat, parser, counter, corrupted, migrationBuffer, keysToMigrate);
        }

        private long initialiseMigrationCounters(AtomicLong counter, AtomicLong corrupted) throws RocksDBException {
            try (TransactionContext context = store.begin()) {
                long keysToMigrate = readOrCountKeysToMigrate(context);
                restoreMigratedCount(context, counter, keysToMigrate);
                restoreCorruptedCount(context, corrupted);
                context.commit();
                return keysToMigrate;
            }
        }

        private long readOrCountKeysToMigrate(TransactionContext context) throws RocksDBException {
            byte[] lastTarget = context.get(store.getDefaultHandle(), LEGACY_MIGRATION_TARGET);
            long keysToMigrate;
            if (lastTarget != null) {
                keysToMigrate = bytesToLong(lastTarget);
            } else {
                LOGGER.info("Determining how many legacy keys need migrating...");
                keysToMigrate = context.count(store.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO));
                context.put(store.getDefaultHandle(), LEGACY_MIGRATION_TARGET, longToBytes(keysToMigrate));
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Legacy store contains {} keys to migrate", humanReadableCount(keysToMigrate));
            }
            return keysToMigrate;
        }

        private void restoreMigratedCount(TransactionContext context, AtomicLong counter, long keysToMigrate)
                throws RocksDBException {
            byte[] lastCount = context.get(store.getDefaultHandle(), LEGACY_MIGRATION_COUNTER);
            if (lastCount == null)
                return;

            counter.set(bytesToLong(lastCount));
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Resuming a previously interrupted partial migration, we previously migrated {} keys [{}]",
                            humanReadableCount(counter), percentage(counter.get(), keysToMigrate));
            }
        }

        private void restoreCorruptedCount(TransactionContext context, AtomicLong corrupted) throws RocksDBException {
            byte[] lastCorruptedCount =
                    context.get(store.getDefaultHandle(), LEGACY_MIGRATION_CORRUPTED_COUNTER);
            if (lastCorruptedCount == null)
                return;

            corrupted.set(bytesToLong(lastCorruptedCount));
            if (corrupted.get() > 0 && LOGGER.isWarnEnabled()) {
                LOGGER.warn("Resuming a previously interrupted partial migration, we previously encountered {} corrupted keys",
                            humanReadableCount(corrupted));
            }
        }

        private void migrateLegacyBatches(MigrationState state) throws RocksDBException {
            boolean complete = false;
            while (!complete) {
                complete = migrateNextBatch(state);
            }
        }

        private boolean migrateNextBatch(MigrationState state) throws RocksDBException {
            try (TransactionContext context = store.beginNested()) {
                byte[] lastKey = context.get(store.getDefaultHandle(), LEGACY_MIGRATION_KEY);
                try (RocksIterator iterator = context.iterator(store.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO))) {
                    if (!positionIteratorForBatch(lastKey, iterator))
                        return true;

                    long batchCount = 0;
                    while (iterator.isValid() && batchCount < MIGRATION_BATCH_SIZE) {
                        migrateCurrentKey(state, iterator);
                        batchCount++;
                    }

                    boolean complete = !iterator.isValid();
                    persistMigrationProgress(context, state, iterator, complete);
                    context.commit();
                    return complete;
                }
            }
        }

        private boolean positionIteratorForBatch(byte[] lastKey, RocksIterator iterator) {
            if (lastKey == null) {
                LOGGER.info("Starting first batch of keys...");
                iterator.seekToFirst();
                return iterator.isValid();
            }

            LOGGER.info("Starting next batch of keys...");
            iterator.seek(lastKey);
            return iterator.isValid();
        }

        private void migrateCurrentKey(MigrationState state, RocksIterator iterator) throws RocksDBException {
            KeyValue keyValue = KeyValue.of(iterator);
            byte[] newKey = migrateKey(keyValue.key(), state.sourceFormat(), state.parser(), state.migrationBuffer(),
                                       store.storeFmt, store.encoder);
            if (newKey == null) {
                recordCorruption(state, iterator);
                return;
            }

            Long newValue = migrateValue(keyValue.value(), state.parser(), state.migrationBuffer());
            if (newValue == null) {
                recordCorruption(state, iterator);
                return;
            }

            store.setLabel(newKey, newValue);
            state.counter().incrementAndGet();
            logMigrationProgress(state);
            iterator.next();
        }

        private void recordCorruption(MigrationState state, RocksIterator iterator) {
            state.counter().incrementAndGet();
            state.corrupted().incrementAndGet();
            iterator.next();
        }

        private void logMigrationProgress(MigrationState state) {
            if (state.counter().get() % 100_000 == 0 && LOGGER.isInfoEnabled()) {
                LOGGER.info("Legacy format migration in progress, migrated {} keys [{}] so far...",
                            humanReadableCount(state.counter()),
                            percentage(state.counter().get(), state.keysToMigrate()));
            }
        }

        private void persistMigrationProgress(TransactionContext context, MigrationState state, RocksIterator iterator,
                                              boolean complete) throws RocksDBException {
            if (!complete) {
                context.put(store.getDefaultHandle(), LEGACY_MIGRATION_KEY,
                            Arrays.copyOf(iterator.key(), iterator.key().length));
            }

            context.put(store.getDefaultHandle(), LEGACY_MIGRATION_COUNTER, longToBytes(state.counter().get()));
            context.put(store.getDefaultHandle(), LEGACY_MIGRATION_CORRUPTED_COUNTER,
                        longToBytes(state.corrupted().get()));
        }

        private void logMigrationSummary(MigrationState state) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Completed legacy format migration, {} labels were migrated [{}]",
                            humanReadableCount(state.counter()), percentage(state.counter().get(), state.keysToMigrate()));
            }
            if (state.corrupted().get() > 0 && LOGGER.isWarnEnabled()) {
                LOGGER.warn("Completed legacy format migration, {} corrupted keys did not have their labels migrated [{}]",
                            humanReadableCount(state.corrupted()),
                            percentage(state.corrupted().get(), state.keysToMigrate()));
            }
        }

        private void validateCorruptionThreshold(File dbPath, MigrationState state) {
            if (exceedsThreshold(state.corrupted().get(), state.counter().get(),
                                 LEGACY_MIGRATION_ACCEPTABLE_CORRUPTION_THRESHOLD)) {
                throw new IllegalStateException(
                        "RocksDB store at " + dbPath.getAbsolutePath() + " contains data in a legacy format which we failed to migrate successfully - too many keys were corrupt (" + percentage(
                                state.corrupted().get(), state.keysToMigrate()) + ")");
            }
        }

        private void completeLegacyMigration() throws RocksDBException {
            try (TransactionContext context = store.begin()) {
                context.put(store.getDefaultHandle(), LEGACY_MIGRATION_KEY, TRUE_BYTES);
                context.put(store.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY,
                            store.storeFmt.toString().getBytes(StandardCharsets.UTF_8));
                LOGGER.info("Committing legacy format migration...");
                context.commit();
                LOGGER.info("Legacy format migration successfully completed!");
            }

            LOGGER.info("Dropping legacy column family...");
            store.dropColumnFamily(store.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO));
            LOGGER.info("Legacy column family dropped successfully");
        }

        private void handleLegacyMigrationFailure(File dbPath, Throwable e) {
            LOGGER.error("Legacy format migration failed/interrupted: ", e);
            store.close();
            if (e instanceof IllegalStateException illegalState) {
                throw illegalState;
            }
            throw new IllegalStateException(
                    "RocksDB store at " + dbPath.getAbsolutePath() + " contains data in a legacy format which we failed to migrate successfully");
        }

        /**
         * Gets whether a calculated percentage exceeds a given threshold
         *
         * @param count     Count
         * @param total     Total from which the percentage will be calculated
         * @param threshold Threshold above which this method should return true
         * @return True if count greater than or equal to total, or the calculated percentage exceeds the given
         * threshold
         */
        private boolean exceedsThreshold(long count, long total, double threshold) {
            if (count >= total) {
                return true;
            }

            double percentage = ((double) count) / ((double) total);
            return percentage >= threshold;
        }

        /**
         * Detects what store format the legacy data was written in, this is permitted to be different from the format
         * configured for this store.
         *
         * @param dbPath Database path
         * @return Legacy store format
         * @throws RocksDBException Thrown if there's a problem accessing RocksDB
         */
        @SuppressWarnings({ "deprecation", "java:S1181" })
        private StoreFmt detectLegacyStorageFormat(File dbPath) throws RocksDBException {
            StoreFmt sourceFormat;
            try (TransactionContext context = store.begin()) {
                byte[] legacyStoreFormat = context.get(store.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY);
                if (legacyStoreFormat == null) {
                    // Most likely it's StoreFmtByString which we can test by inspecting the first key and trying to
                    // parse it
                    StoreFmtByString byString = new StoreFmtByString();
                    try (RocksIterator iterator = context.iterator(store.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO))) {
                        iterator.seekToFirst();
                        try {
                            ByteBuffer buffer =
                                    ByteBuffer.allocate(iterator.key().length).order(ByteOrder.LITTLE_ENDIAN);
                            buffer.put(iterator.key());
                            byString.createParser().parseTriple(buffer.flip(), new ArrayList<>());
                            sourceFormat = byString;
                            LOGGER.info(
                                    "Legacy store had never recorded its store format, detected that it was using StoreFmtByString");
                        } catch (Throwable e) {
                            // If we can't parse the key as a triple then it's almost certainly StoreFmtByHash BUT we
                            // don't know what hash so have to assume it matches our current configuration
                            sourceFormat = store.storeFmt;
                            LOGGER.warn(
                                    "Legacy store had never recorded its store format, attempting migration under the assumption that it matches the StoreFmtByHash configured for this store");
                        }
                    }

                } else if (Objects.equals(new String(legacyStoreFormat, StandardCharsets.UTF_8),
                                          StoreFmtByString.class.getSimpleName())) {
                    sourceFormat = new StoreFmtByString();
                    LOGGER.info("Legacy store used StoreFmtByString, will migrate keys to use {}", store.storeFmt);
                } else {
                    sourceFormat = store.storeFmt;
                    store.verifyStoreFormat(dbPath, legacyStoreFormat);
                    LOGGER.info(
                            "Legacy store used {} which matches our configuration, only partial key migration required",
                            sourceFormat);
                }
            }
            return sourceFormat;
        }

        /**
         * Migrates a key
         *
         * @param key             Key to migrate
         * @param sourceFormat    Source format
         * @param parser          Source format parser
         * @param migrationBuffer Migration buffer
         * @param targetFormat    Target format
         * @param encoder         Target format encoder
         * @return Migrated key bytes or {@code null} if a corrupted key is encountered
         */
        @SuppressWarnings({ "deprecation", "java:S1181" })
        private byte[] migrateKey(byte[] key, StoreFmt sourceFormat, StoreFmt.Parser parser, ByteBuffer migrationBuffer,
                                  StoreFmt targetFormat,
                                  StoreFmt.Encoder encoder) {
            if (sourceFormat == targetFormat && sourceFormat instanceof StoreFmtByHash hashFormat) {
                // NB - Verify that the existing key has the expected length, the key could be legitimately shorter than
                //      this depending on the hash function used and whether the Hasher tries to compress the hash by
                //      omitting empty bytes
                int expectedKeyLength = 3 * hashFormat.getHasher().sizeInBytes();
                if (key.length > expectedKeyLength) {
                    LOGGER.warn(
                            "Wrong length key encountered for StoreFmtByHash, expected keys to be of length {} bytes but got key of length {} bytes",
                            expectedKeyLength, key.length);
                    return null;
                }

                // Legacy store only hashed subject, predicate and object whereas modern store also hashes the graph
                // Luckily each element is independently hashed and appended together to generate the key we can migrate
                // the key by simply hashing the default graph node and appending it to the front of the existing key to
                // form the key as it is expected to exist in the modern store
                ByteBuffer buffer = store.keyBuffer.get().clear();
                if (defaultGraphBytes.length + key.length > buffer.limit()) {
                    LOGGER.warn(
                            "Too long key encountered for StoreFmtByHash, expected keys to be no longer than {} bytes but got {} bytes",
                            buffer.limit(), defaultGraphBytes.length + key.length);
                    return null;
                }
                buffer.put(defaultGraphBytes);
                buffer.put(key);
                return asByteArray(buffer.flip());
            }

            // Otherwise assume that we can parse and then encode the triple key as a quad key in the default graph to
            // get the new key under which it should be stored
            List<Node> spo = new ArrayList<>();
            ByteBuffer buffer = migrationBuffer.clear();
            buffer.put(key);
            try {
                parser.parseTriple(buffer.flip(), spo);
                buffer.clear();
                encoder.formatQuad(buffer, Quad.defaultGraphIRI, spo.get(0), spo.get(1), spo.get(2));
            } catch (Throwable e) {
                LOGGER.warn("Corrupted/too large key encountered ({} bytes), ignored and not migrated", key.length);
                return null;
            }
            return asByteArray(buffer.flip());
        }

        /**
         * Migrates a value
         *
         * @param value  Value (label) to migrate
         * @param parser Source format parser
         * @param buffer Migration buffer
         * @return Label ID for the migrated label
         */
        @SuppressWarnings({ "deprecation", "java:S1181" })
        private Long migrateValue(byte[] value, StoreFmt.Parser parser, ByteBuffer buffer) {
            Collection<Label> labels = new HashSet<>();
            buffer.clear();
            buffer.put(value);
            try {
                parser.parseLabels(buffer.flip(), labels);
            } catch (Throwable e) {
                LOGGER.warn("Corrupted labels encountered, ignored for migration: {}", e.getMessage());
                return null;
            }
            if (labels.size() != 1) {
                throw new IllegalStateException(
                        "Cannot migrate from legacy storage that has multiple distinct labels (" + labels.size() + ") associated with triples");
            }

            // Dictionary encode the label
            byte[] label = labels.iterator().next().getData();
            return store.idForLabel(label);
        }
    }
}
