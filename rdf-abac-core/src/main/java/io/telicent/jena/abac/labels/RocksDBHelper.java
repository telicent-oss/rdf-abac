package io.telicent.jena.abac.labels;

import org.rocksdb.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.telicent.jena.abac.labels.Labels.LOG;

public class RocksDBHelper {

    private final static String CREATE_MESSAGE = "creation of RocksDB label store";

    private final AtomicBoolean openFlag = new AtomicBoolean(false);

    private final List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<>();

    private RocksDB db;

    /**
     * Returns a new instance of RocksDB
     * @param dbPath the path to the database
     * @return the RocksDB instance
     */
    public RocksDB openDB(final String dbPath) {
        final ColumnFamilyOptions columnFamilyOptions = configureRocksDBColumnFamilyOptions().setMergeOperator(new StringAppendOperator(""));

        final var defaultDescriptor = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, columnFamilyOptions);
        final var cfhSPODescriptor = new ColumnFamilyDescriptor("CF_ABAC_SPO".getBytes(), columnFamilyOptions);
        // TODO (AP) we need a native comparator if we are to have any kind of
        // comparator. The performance of Java-based comparators is far too poor for our use case
        // a comparator is necessary to ensure S,P,O and S,P,Any are close in lookup
        // which may improve performance but is probably not vital...
        final var cfhS__Descriptor = new ColumnFamilyDescriptor("CF_ABAC_S**".getBytes(), columnFamilyOptions);
        final var cfh_P_Descriptor = new ColumnFamilyDescriptor("CF_ABAC_*P*".getBytes(), columnFamilyOptions);
        final var cfh___Descriptor = new ColumnFamilyDescriptor("CF_ABAC_***".getBytes(), columnFamilyOptions);

        if ( !openFlag.compareAndSet(false, true) ) {
            throw new RuntimeException("Race condition during " + CREATE_MESSAGE);
        }

        try (final DBOptions dbOptions = configureRocksDBOptions().setCreateIfMissing(true).setCreateMissingColumnFamilies(true)) {
            List<ColumnFamilyDescriptor> columnFamilyDescriptorList = List.of(defaultDescriptor, cfhSPODescriptor, cfhS__Descriptor,
                    cfh_P_Descriptor, cfh___Descriptor);

            db = RocksDB.open(dbOptions, dbPath, columnFamilyDescriptorList, columnFamilyHandleList);
            LOG.debug("Opened RocksDB instance:{}", db);
            return db;
        } catch (RocksDBException e) {
            if ( !openFlag.compareAndSet(true, false) ) {
                throw new RuntimeException("Race condition during failing " + CREATE_MESSAGE);
            }
            LOG.error("Unable to open/create RocksDB label store: {}", dbPath, e);
            throw new RuntimeException("Failed " + CREATE_MESSAGE, e);
        }
    }

    public void closeDB() {
        if ( openFlag.compareAndSet(true, false) ) {
            try {
                LOG.debug("Closing RocksDB instance:{}",db);
                db.closeE();
            } catch ( RocksDBException rocksDBException) {
                LOG.error("Problem encountered closing RocksDB instance", rocksDBException);
            }
        }
    }

    /**
     * Set up performance tuning options for column family options as recommended by
     * <a href=
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
     * Set up performance tuning options for database options as recommended by
     * <a href=
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
     * @return - the transactional RocksDB
     */
    public TransactionalRocksDB getTransactionalRocksDB() {
        return new TransactionalRocksDB(db);
    }

    /**
     * Accessor to the ColumnFamilyHandle used within the RocksDB instance
     * @param index the column index to remove and return
     * @return the removed ColumnFamilyHandle
     */
    public ColumnFamilyHandle removeFromColumnFamilyHandleList(int index) {
        return columnFamilyHandleList.remove(index);
    }

}
