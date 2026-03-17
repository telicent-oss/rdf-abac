package io.telicent.jena.abac.labels.store.rocksdb.legacy;

import com.google.j2objc.annotations.RetainedWith;
import io.telicent.jena.abac.labels.StoreFmt;
import org.rocksdb.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.telicent.jena.abac.labels.Labels.LOG;

public class RocksDBHelper {

    /**
     * Key stored in the default column family to record the store format used so subsequent store opens can verify that
     * the configured store format matches that previously used
     */
    public static final byte[] STORE_FORMAT_KEY = StoreFmt.class.getSimpleName().getBytes(StandardCharsets.UTF_8);
    private final static String CREATE_MESSAGE = "creation of RocksDB label store";
    /**
     * Name of the column family used to store the triple to label mapping in the {@link LegacyLabelsStoreRocksDB}
     * implementation
     */
    public static final byte[] COLUMN_FAMILY_SPO = "CF_ABAC_SPO".getBytes(StandardCharsets.UTF_8);

    /**
     * Name of the deprecated column family that was historically used to store subject based wildcard pattern matches,
     * retained purely to allow migrating from legacy stores as RocksDB requires us to open all pre-existing column
     * families
     */
    @Deprecated
    public static final byte[] COLUMN_FAMILY_S = "CF_ABAC_S**".getBytes(StandardCharsets.UTF_8);

    /**
     * Name of the deprecated column family that was historically used to store predicate based wildcard pattern
     * matches, retained purely to allow migrating from legacy stores as RocksDB requires us to open all pre-existing
     * column families
     */
    @Deprecated
    public static final byte[] COLUMN_FAMILY_P = "CF_ABAC_*P*".getBytes(StandardCharsets.UTF_8);

    /**
     * Name of the deprecated column family that was historically used to store wildcard pattern matches, retained
     * purely to allow migrating from legacy stores as RocksDB requires us to open all pre-existing column families
     */
    @Deprecated
    public static final byte[] COLUMN_FAMILY_WILDCARDS = "CF_ABAC_***".getBytes(StandardCharsets.UTF_8);

    /**
     * List of the legacy column families that must be opened in order to successfully open a legacy format store
     */
    public static final List<byte[]> LEGACY_COLUMN_FAMILIES =
            List.of(COLUMN_FAMILY_SPO, COLUMN_FAMILY_S, COLUMN_FAMILY_P, COLUMN_FAMILY_WILDCARDS);

    private final AtomicBoolean openFlag = new AtomicBoolean(false);

    private final List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<>();

    private RocksDB db;

    /**
     * Returns a new instance of RocksDB
     *
     * @param dbPath the path to the database
     * @return the RocksDB instance
     */
    public RocksDB openDB(final String dbPath) {
        final ColumnFamilyOptions columnFamilyOptions =
                configureRocksDBColumnFamilyOptions().setMergeOperator(new StringAppendOperator(""));

        final var defaultDescriptor = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, columnFamilyOptions);
        final var cfhSPODescriptor = new ColumnFamilyDescriptor(COLUMN_FAMILY_SPO, columnFamilyOptions);
        final var cfhSDescriptor = new ColumnFamilyDescriptor(COLUMN_FAMILY_S, columnFamilyOptions);
        final var cfhPDescriptor = new ColumnFamilyDescriptor(COLUMN_FAMILY_P, columnFamilyOptions);
        final var cfhWildcardsDescription = new ColumnFamilyDescriptor(COLUMN_FAMILY_WILDCARDS, columnFamilyOptions);

        if (!openFlag.compareAndSet(false, true)) {
            throw new RuntimeException("Race condition during " + CREATE_MESSAGE);
        }

        try (final DBOptions dbOptions = configureRocksDBOptions().setCreateIfMissing(true)
                                                                  .setCreateMissingColumnFamilies(true)) {
            List<ColumnFamilyDescriptor> columnFamilyDescriptorList =
                    List.of(defaultDescriptor, cfhSPODescriptor, cfhSDescriptor, cfhPDescriptor,
                            cfhWildcardsDescription);

            db = RocksDB.open(dbOptions, dbPath, columnFamilyDescriptorList, columnFamilyHandleList);
            LOG.debug("Opened RocksDB instance:{}", db);
            return db;
        } catch (RocksDBException e) {
            if (!openFlag.compareAndSet(true, false)) {
                throw new RuntimeException("Race condition during failing " + CREATE_MESSAGE);
            }
            LOG.error("Unable to open/create RocksDB label store: {}", dbPath, e);
            throw new RuntimeException("Failed " + CREATE_MESSAGE, e);
        }
    }

    public void closeDB() {
        if (openFlag.compareAndSet(true, false)) {
            try {
                LOG.debug("Closing RocksDB instance:{}", db);
                db.closeE();
            } catch (RocksDBException rocksDBException) {
                LOG.error("Problem encountered closing RocksDB instance", rocksDBException);
            }
        }
    }

    /**
     * Set up performance tuning options for column family options as recommended by <a href=
     * "https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning">Setup-Options-and-Basic-Tuning</a>
     *
     * @return column family options configured as recommended
     */
    protected ColumnFamilyOptions configureRocksDBColumnFamilyOptions() {
        var options = new ColumnFamilyOptions();
        options.setLevelCompactionDynamicLevelBytes(true);
        options.setCompressionType(CompressionType.LZ4_COMPRESSION);
        options.setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION);

        return options;
    }

    /**
     * Set up performance tuning options for database options as recommended by <a href=
     * "https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning">Setup-Options-and-Basic-Tuning</a>
     *
     * @return database options configured as recommended
     */
    protected DBOptions configureRocksDBOptions() {
        var options = new Options();

        LOG.debug("Configure RocksDB options from defaults to recommended:");
        LOG.debug("maxBackgroundJobs {} to {}", options.maxBackgroundJobs(), 6);
        options.setMaxBackgroundJobs(6);
        LOG.debug("bytesPerSync {} to {}", options.bytesPerSync(), 1048576);
        options.setBytesPerSync(1048576);
        LOG.debug("compactionPriority {} to {}", options.compactionPriority(), CompactionPriority.MinOverlappingRatio);
        options.setCompactionPriority(CompactionPriority.MinOverlappingRatio);

        var tableOptions = new BlockBasedTableConfig();
        LOG.debug("blockSize {} to {}", tableOptions.blockSize(), 16 * 1024);
        tableOptions.setBlockSize(16 * 1024);
        LOG.debug("cacheIndexAndFilterBlocks {} to {}", tableOptions.cacheIndexAndFilterBlocks(), true);
        tableOptions.setCacheIndexAndFilterBlocks(true);
        LOG.debug("pinL0FilterAndIndexBlocksInCache {} to {}", tableOptions.pinL0FilterAndIndexBlocksInCache(), true);
        tableOptions.setPinL0FilterAndIndexBlocksInCache(true);
        var newFilterPolicy = new BloomFilter(10.0);
        LOG.debug("filterPolicy {} to {}", tableOptions.filterPolicy(), newFilterPolicy);
        tableOptions.setFilterPolicy(newFilterPolicy);
        LOG.debug("formatVersion {} to {}", tableOptions.formatVersion(), 5);
        tableOptions.setFormatVersion(5);

        options.setTableFormatConfig(tableOptions);

        return new DBOptions(options);
    }

    /**
     * Returns a transactional instance based on the underlying RocksDB instance
     *
     * @return - the transactional RocksDB
     */
    public TransactionalRocksDB getTransactionalRocksDB() {
        return new TransactionalRocksDB(db);
    }

    /**
     * Accessor to the ColumnFamilyHandle used within the RocksDB instance
     *
     * @param index the column index to remove and return
     * @return the removed ColumnFamilyHandle
     */
    public ColumnFamilyHandle removeFromColumnFamilyHandleList(int index) {
        return columnFamilyHandleList.remove(index);
    }

}
