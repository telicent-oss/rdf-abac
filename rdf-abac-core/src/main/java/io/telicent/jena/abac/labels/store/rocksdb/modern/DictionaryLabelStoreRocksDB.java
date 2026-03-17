package io.telicent.jena.abac.labels.store.rocksdb.modern;

import io.telicent.jena.abac.labels.*;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
import io.telicent.smart.cache.storage.labels.rocksdb.RocksDbLabelsStore;
import io.telicent.smart.cache.storage.rocksdb.TransactionContext;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.RocksDBException;
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
 * created using either {@link StoreFmtByString} or {@link StoreFmtByHash} are supported for migration.  If your legacy
 * store uses {@link StoreFmtByString} you <strong>MUST</strong> open it first with the legacy implementation to
 * populate the necessary store format metadata in order for migration to proceed correctly.  This unfortunate
 * limitation comes from the fact that prior to {@code 3.0.0} the store did not record any format metadata in itself so
 * unless you open it with the correct store format once using the legacy store implementation this code can't detect
 * the correct store format and migrate data safely.  However, in this scenario if you attempt to open a legacy store
 * immediately with this implementation then it will fail.
 * </p>
 */
public class DictionaryLabelStoreRocksDB extends RocksDbLabelsStore implements LabelsStore {

    private static final Logger LOGGER = LoggerFactory.getLogger(DictionaryLabelStoreRocksDB.class);

    private static final int MAX_HASH_LENGTH = 512 / 8;
    public static final byte[] LEGACY_MIGRATION_KEY = "legacyMigration".getBytes(StandardCharsets.UTF_8);
    public static final byte[] TRUE_BYTES = "true".getBytes(StandardCharsets.UTF_8);

    private final StoreFmt storeFmt;
    private final StoreFmt.Encoder encoder;
    private final StoreFmt.Parser parser;
    private final JenaTransactionWrapper wrapper;

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
            throw new IllegalArgumentException("Only StoreFmtByHash is currently supported");
        }
        this.encoder = this.storeFmt.createEncoder();
        this.parser = this.storeFmt.createParser();
        this.wrapper = new JenaTransactionWrapper(this);

        // Detect whether we've been asked to open a legacy store
        try (TransactionContext context = this.beginNested()) {
            if (!context.isEmpty(this.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO))) {
                LOGGER.info(
                        "RocksDB store at {} contains data in a legacy format, checking whether automatic migration is needed...",
                        dbPath.getAbsolutePath());

                // Check the legacyMigration key, if this is set then migration has happened previously and need not happen again
                byte[] migrated =
                        context.get(this.getDefaultHandle(), LEGACY_MIGRATION_KEY);
                if (!Arrays.equals(migrated, TRUE_BYTES)) {
                    migrateLegacyStorage(dbPath, context);
                } else {
                    LOGGER.info("Legacy format migration has previously occurred");
                }
            }
        }

        // Validate Store Format matches
        try (TransactionContext context = this.begin()) {
            byte[] storeFormat = context.get(this.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY);
            if (storeFormat == null) {
                context.put(this.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY, storeFmt.toString().getBytes(
                        StandardCharsets.UTF_8));
                context.commit();
            } else {
                verifyStoreFormat(dbPath, storeFormat);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void migrateLegacyStorage(File dbPath, TransactionContext context) throws RocksDBException {
        // Need to find the previous storage format (if recorded) to determine our source format for migration
        byte[] legacyStoreFormat = context.get(this.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY);
        StoreFmt sourceFormat;
        if (legacyStoreFormat == null) {
            sourceFormat = this.storeFmt;
            LOGGER.warn(
                    "Legacy store had never recorded its store format, attempting migration under the assumption that it matches the format configured for this store");
        } else if (Objects.equals(new String(legacyStoreFormat, StandardCharsets.UTF_8),
                                  StoreFmtByString.class.getSimpleName())) {
            sourceFormat = new StoreFmtByString();
            LOGGER.info("Legacy store used StoreFmtByString, will migrate keys to use {}", this.storeFmt);
        } else {
            sourceFormat = this.storeFmt;
            verifyStoreFormat(dbPath, legacyStoreFormat);
            LOGGER.info("Legacy store used {} which matches our configuration, no key migration required",
                        sourceFormat);
        }

        // Iterate over the legacy column family and migrate the key values
        StoreFmt.Parser parser = sourceFormat.createParser();
        AtomicLong counter = new AtomicLong(0);
        ByteBuffer migrationBuffer = ByteBuffer.allocate(LegacyLabelsStoreRocksDB.DEFAULT_BUFFER_CAPACITY * 10)
                                           .order(ByteOrder.LITTLE_ENDIAN);
        LOGGER.info("Beginning legacy format migration...");
        try {
            context.forEach(this.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO), kv -> {
                byte[] newKey = migrateKey(kv.key(), sourceFormat, parser, migrationBuffer, this.storeFmt, this.encoder);
                long newValue = migrateValue(kv.value(), parser, migrationBuffer);
                this.setLabel(newKey, newValue);
                counter.incrementAndGet();
            });
            LOGGER.info("Completed legacy format migration, {} labels were migrated", counter.get());

            // Upon successful migration set the legacyMigration key to true
            // And update the store format key to match our current format (which may differ from the legacy format)
            context.put(this.getDefaultHandle(), LEGACY_MIGRATION_KEY, TRUE_BYTES);
            context.put(this.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY, this.storeFmt.toString().getBytes(
                    StandardCharsets.UTF_8));

            // Only if we reach safely here do we commit() our changes
            LOGGER.info("Committing legacy format migration...");
            context.commit();
            LOGGER.info("Legacy format migration successfully completed!");

            // Upon successfully commiting we can drop the legacy column family we migrated to reclaim the no longer
            // needed disk space
            LOGGER.info("Dropping legacy column family...");
            this.dropColumnFamily(this.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO));
            LOGGER.info("Legacy column family dropped successfully");
        } catch (RocksDBException e) {
            LOGGER.error("Legacy format migration failed: ", e);
            this.close();
            throw new IllegalStateException(
                    "RocksDB store at " + dbPath.getAbsolutePath() + " contains data in a legacy format which we failed to migrate successfully");
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

    @SuppressWarnings("deprecation")
    private byte[] migrateKey(byte[] key, StoreFmt sourceFormat, StoreFmt.Parser parser, ByteBuffer migrationBuffer,
                              StoreFmt targetFormat,
                              StoreFmt.Encoder encoder) {
        if (sourceFormat == targetFormat && sourceFormat instanceof StoreFmtByHash) {
            // Legacy store only hashed subject, predicate and object whereas modern store also hashes the graph
            // Luckily each element is independently hashed and appended together to generate the key we can migrate
            // the key by simply hashing the default graph node and appending it to the front of the existing key to
            // form the key as it is expected to exist in the modern store
            ByteBuffer buffer = this.keyBuffer.get().clear();
            encoder.formatSingleNode(buffer, Quad.defaultGraphIRI);
            buffer.put(key);
            return asByteArray(buffer.flip());
        }

        // Otherwise assume that we can parse and then encode the triple key as a quad key in the default graph to get
        // the new key under which it should be stored
        List<Node> spo = new ArrayList<>();
        ByteBuffer buffer = migrationBuffer.clear();
        buffer.put(key);
        parser.parseTriple(buffer.flip(), spo);
        buffer.clear();
        encoder.formatQuad(buffer, Quad.defaultGraphIRI, spo.get(0), spo.get(1), spo.get(2));
        return asByteArray(buffer.flip());
    }

    @SuppressWarnings("deprecation")
    private long migrateValue(byte[] value, StoreFmt.Parser parser, ByteBuffer buffer) {
        List<Label> labels = new ArrayList<>();
        buffer.clear();
        buffer.put(value);
        parser.parseLabels(buffer.flip(), labels);
        if (labels.size() != 1) {
            throw new IllegalStateException(
                    "Cannot migrate from legacy storage that has multiple labels (" + labels.size() + ") associated with triples");
        }

        // Dictionary encode the label
        byte[] label = labels.getFirst().getData();
        return this.idForLabel(label);
    }

    @Override
    protected List<ColumnFamilyDescriptor> prepareColumnFamilyDescriptors(ColumnFamilyOptions cfOptions) {
        List<ColumnFamilyDescriptor> descriptors = new ArrayList<>(super.prepareColumnFamilyDescriptors(cfOptions));

        // Add the legacy column families, if the SPO family contains data then we will automatically migrate it the
        // first time we're asked to read a legacy store with this column family non-empty
        for (byte[] name : RocksDBHelper.LEGACY_COLUMN_FAMILIES) {
            descriptors.add(new ColumnFamilyDescriptor(name, cfOptions));
        }
        return descriptors;
    }

    /**
     * Thread local byte buffers for encoding keys
     */
    private final ThreadLocal<ByteBuffer> keyBuffer =
            ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(4 * MAX_HASH_LENGTH).order(
                    ByteOrder.LITTLE_ENDIAN));

    @Override
    public Label labelForQuad(Quad quad) {
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
        return label != null ? new Label(label, StandardCharsets.UTF_8) : null;
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

        // Store the label and associated the label with this quad as a single atomic transaction
        // Calling beginNested() ensures that when the called methods call begin() they share the same transaction
        // rather than performing their actions in independent transactions
        try (TransactionContext context = this.beginNested()) {
            long labelId = this.idForLabel(label.getData());
            this.setLabel(key, labelId);

            context.commit();
        } catch (RocksDBException e) {
            throw new LabelsException("Failed to store label in RocksDB", e);
        }
    }

    @Override
    public void remove(Quad quad) {
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
     * This is a thread-safe singleton (since transactions in Jena are thread scoped).
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
                throw new IllegalStateException("Already in a transaction");
            }
        }

        private void verifyTransaction() {
            if (!this.isInTransaction()) {
                throw new IllegalStateException("Not in a transaction");
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
                throw new RuntimeException(e);
            }
        }

        @Override
        public void abort() {
            verifyTransaction();
            this.context.get().close();
        }

        @Override
        public void end() {
            verifyTransaction();
            this.context.get().close();
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
}
