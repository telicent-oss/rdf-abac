package io.telicent.jena.abac.labels.store.rocksdb.modern;

import io.telicent.jena.abac.labels.*;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
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
        // This is an intentional choice.  The reasoning is that if we allow removing labels a malicious attacker can
        // downgrade/remove labels by sending patches that first deletes quads, and then re-adds them without a security
        // label
        // However this does have a storage cost over time so we may revisit this in the future
        throw new UnsupportedOperationException("Removing labels is not supported");
    }

    @Override
    public boolean isEmpty() {
        try (TransactionContext context = this.begin()) {
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
        return Map.of();
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

        /**
         * Creates a new transaction wrapper
         *
         * @param store Store
         */
        JenaTransactionWrapper(DictionaryLabelStoreRocksDB store) {
            this.store = store;
            this.context = ThreadLocal.withInitial(() -> null);
        }

        @Override
        public void begin(TxnType type) {
            verifyNoTransaction();
            this.context.set(this.store.beginNested());
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
            // We consider all transactions to be write transactions so this MUST always return true per Jena API
            // contract
            return true;
        }

        @Override
        public void commit() {
            verifyTransaction();
            try {
                this.context.get().commit();
            } catch (RocksDBException e) {
                throw new JenaTransactionException(e);
            }
        }

        @Override
        public void abort() {
            verifyTransaction();
            this.context.get().close();
            // If a transaction aborts then need to clear out the cache otherwise changes will leak beyond the aborted
            // transaction
            store.labelCache.clear();
        }

        @Override
        public void end() {
            // The Jena contract (at least when using its Txn helper) is that end() always gets called even after a
            // commit()/abort()
            if (isInTransaction()) {
                // If a transaction ends without a commit we need to treat this as an abort and clear the label
                // cache otherwise changes will leak beyond the aborted transaction
                store.labelCache.clear();
                this.context.get().close();
            }
        }

        @Override
        public ReadWrite transactionMode() {
            return isInTransaction() ? ReadWrite.WRITE : null;
        }

        @Override
        public TxnType transactionType() {
            return isInTransaction() ? TxnType.WRITE : null;
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

        /**
         * The migration batch size, i.e. how many keys do we migrate in a single RocksDB transaction.  This is a
         * balance between frequency of commits (to ensure durability of the migration) and the memory overheads of a
         * transaction.  Experimentation has shown 1 million keys to be a reasonable number which achieves a migration
         * throughput of roughly 3.5 million keys per minute on a representative production label store.
         */
        public static final int MIGRATION_BATCH_SIZE = 1_000_000;
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

        /**
         * Migrates data from the legacy store format to the current format
         *
         * @param dbPath Database path
         * @throws RocksDBException Thrown if there is a problem performing RocksDB operations
         */
        @SuppressWarnings("deprecation")
        private void migrateLegacyStorage(File dbPath) throws RocksDBException {
            // Need to find the previous storage format (if recorded) to determine our source format for migration
            StoreFmt sourceFormat = detectLegacyStorageFormat(dbPath);

            // Iterate over the legacy column family and migrate the key values
            StoreFmt.Parser parser = sourceFormat.createParser();
            AtomicLong counter = new AtomicLong(0);
            ByteBuffer migrationBuffer = ByteBuffer.allocate(LegacyLabelsStoreRocksDB.DEFAULT_BUFFER_CAPACITY * 10)
                                                   .order(ByteOrder.LITTLE_ENDIAN);
            LOGGER.info("Beginning legacy format migration...");
            try {
                boolean complete = false;
                long keysToMigrate = 0;
                try (TransactionContext context = store.begin()) {
                    // We remember our target so that if we get interrupted and are resuming a migration we don't have
                    // to perform a count of the legacy column family before restarting
                    // This works because once we have begun migration the store cannot be opened with the old
                    // implementation class so we guarantee that the number of keys to migrate cannot change
                    byte[] lastTarget = context.get(store.getDefaultHandle(), LEGACY_MIGRATION_TARGET);
                    if (lastTarget != null) {
                        keysToMigrate = bytesToLong(lastTarget);
                    } else {
                        LOGGER.info("Determining how many legacy keys need migrating...");
                        keysToMigrate = context.count(store.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO));
                        context.put(store.getDefaultHandle(), LEGACY_MIGRATION_TARGET, longToBytes(keysToMigrate));
                    }
                    LOGGER.info("Legacy store contains {} keys to migrate", String.format("%,d", keysToMigrate));

                    // We also remember how many keys we have successfully migrated so far.  This allows us to ensure we
                    // are reporting accurate migration progression even when resuming a partial migration
                    byte[] lastCount = context.get(store.getDefaultHandle(), LEGACY_MIGRATION_COUNTER);
                    if (lastCount != null) {
                        counter.set(bytesToLong(lastCount));
                        LOGGER.info(
                                "Resuming a previously interrupted partial migration, we previously migrated {} keys [{}]",
                                String.format("%,d", counter.get()), percentage(counter.get(), keysToMigrate));
                    }

                    context.commit();
                }

                while (!complete) {
                    // Keys are migrated in batches controlled by MIGRATION_BATCH_SIZE
                    // This means that we commit the migration progress regularly, this ensures that the transaction
                    // overheads don't get too large to impact read/write performance.  Plus should we get
                    // interrupted during a migration we can resume that migration without re-processing the already
                    // migrated keys
                    try (TransactionContext context = store.beginNested()) {
                        // The next migration key is stored in the default column family so if we are aborted partway
                        // through a migration we can cleanly resume it
                        byte[] lastKey = context.get(store.getDefaultHandle(), LEGACY_MIGRATION_KEY);
                        try (RocksIterator iterator = context.iterator(
                                store.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO))) {
                            // Move to either the first or next key depending on whether this is our first time around
                            // the migration loop
                            if (lastKey == null) {
                                LOGGER.info("Starting first batch of keys...");
                                iterator.seekToFirst();
                            } else {
                                LOGGER.info("Starting next batch of keys...");
                                iterator.seek(lastKey);
                                if (!iterator.isValid()) {
                                    break;
                                }
                            }
                            KeyValue kv = KeyValue.of(iterator);

                            // Actual key migration loop
                            // Continue until we've either hit the batch size or the end of the iterator
                            long batchCount = 0;
                            while (iterator.isValid() && batchCount < MIGRATION_BATCH_SIZE) {
                                byte[] newKey =
                                        migrateKey(kv.key(), sourceFormat, parser, migrationBuffer, store.storeFmt,
                                                   store.encoder);
                                long newValue = migrateValue(kv.value(), parser, migrationBuffer);
                                store.setLabel(newKey, newValue);
                                counter.incrementAndGet();
                                batchCount++;

                                if (counter.get() % 100_000 == 0) {
                                    LOGGER.info("Legacy format migration in progress, migrated {} keys [{}] so far...",
                                                String.format("%,d", counter.get()),
                                                percentage(counter.get(), keysToMigrate));
                                }

                                iterator.next();
                            }

                            complete = !iterator.isValid();
                            if (!complete) {
                                // Store the last key we processed so that next time round the loop we'll resume
                                // migration from that point
                                context.put(store.getDefaultHandle(), LEGACY_MIGRATION_KEY,
                                            Arrays.copyOf(kv.key(), kv.key().length));
                            }

                            // We also store the count of how many keys we've migrated so far so that if we get
                            // interrupted and later need to resume we can report this while providing an accurate
                            // count
                            context.put(store.getDefaultHandle(), LEGACY_MIGRATION_COUNTER, longToBytes(counter.get()));
                        }

                        // Commit the current batch of migrated data
                        context.commit();
                    }
                }
                LOGGER.info("Completed legacy format migration, {} labels were migrated [{}]",
                            String.format("%,d", counter.get()), percentage(counter.get(), keysToMigrate));

                // Upon successful migration set the legacyMigration key to true
                // And update the store format key to match our current format (which may differ from the legacy format)
                try (TransactionContext context = store.begin()) {
                    context.put(store.getDefaultHandle(), LEGACY_MIGRATION_KEY, TRUE_BYTES);
                    context.put(store.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY,
                                store.storeFmt.toString().getBytes(
                                        StandardCharsets.UTF_8));
                    LOGGER.info("Committing legacy format migration...");
                    context.commit();
                    LOGGER.info("Legacy format migration successfully completed!");
                }

                // Upon successfully commiting we can drop the legacy column family we migrated to reclaim the no longer
                // needed disk space
                LOGGER.info("Dropping legacy column family...");
                store.dropColumnFamily(store.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO));
                LOGGER.info("Legacy column family dropped successfully");
            } catch (Throwable e) {
                LOGGER.error("Legacy format migration failed/interrupted: ", e);
                store.close();
                throw new IllegalStateException(
                        "RocksDB store at " + dbPath.getAbsolutePath() + " contains data in a legacy format which we failed to migrate successfully");
            }
        }

        private String percentage(long migratedSoFar, long totalToMigrate) {
            if (migratedSoFar == totalToMigrate) {
                return "100%";
            } else {
                double percentage = (double) migratedSoFar / (double) totalToMigrate;
                return String.format("%.2f", percentage * 100) + "%";
            }
        }

        /**
         * Detects what store format the legacy data was written in, this is permitted to be different from the format
         * configured for this store.
         *
         * @param dbPath Database path
         * @return Legacy store format
         * @throws RocksDBException Thrown if there's a problem accessing RocksDB
         */
        @SuppressWarnings("deprecation")
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
         * @return Migrated key bytes
         */
        @SuppressWarnings("deprecation")
        private byte[] migrateKey(byte[] key, StoreFmt sourceFormat, StoreFmt.Parser parser, ByteBuffer migrationBuffer,
                                  StoreFmt targetFormat,
                                  StoreFmt.Encoder encoder) {
            if (sourceFormat == targetFormat && sourceFormat instanceof StoreFmtByHash) {
                // Legacy store only hashed subject, predicate and object whereas modern store also hashes the graph
                // Luckily each element is independently hashed and appended together to generate the key we can migrate
                // the key by simply hashing the default graph node and appending it to the front of the existing key to
                // form the key as it is expected to exist in the modern store
                ByteBuffer buffer = store.keyBuffer.get().clear();
                buffer.put(defaultGraphBytes);
                buffer.put(key);
                return asByteArray(buffer.flip());
            }

            // Otherwise assume that we can parse and then encode the triple key as a quad key in the default graph to
            // get the new key under which it should be stored
            List<Node> spo = new ArrayList<>();
            ByteBuffer buffer = migrationBuffer.clear();
            buffer.put(key);
            parser.parseTriple(buffer.flip(), spo);
            buffer.clear();
            encoder.formatQuad(buffer, Quad.defaultGraphIRI, spo.get(0), spo.get(1), spo.get(2));
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
        @SuppressWarnings("deprecation")
        private long migrateValue(byte[] value, StoreFmt.Parser parser, ByteBuffer buffer) {
            Collection<Label> labels = new HashSet<>();
            buffer.clear();
            buffer.put(value);
            parser.parseLabels(buffer.flip(), labels);
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
