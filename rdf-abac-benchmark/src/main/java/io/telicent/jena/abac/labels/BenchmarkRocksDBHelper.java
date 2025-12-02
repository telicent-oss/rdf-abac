package io.telicent.jena.abac.labels;

import org.rocksdb.*;

import static io.telicent.jena.abac.labels.Labels.LOG;

/**
 * Benchmark-only helper that allows us to vary RocksDB compression settings.
 *
 * Production code continues to use RocksDBHelper directly; benchmarks use this
 * subclass so that we can sweep options via JMH @Param values.
 */
class BenchmarkRocksDBHelper extends RocksDBHelper {

    private CompressionType compressionType;
    private CompressionType bottommostCompressionType;
    private Integer blockSizeBytes;

    private Boolean optimizeFiltersForMemory;
    private Boolean setCacheIndexAndFilterBlocks;
    private Boolean pinL0FilterAndIndexBlocksInCache;

    private Integer maxBackgroundJobs;
    private Long bytesPerSync;


    private Statistics statistics;

    BenchmarkRocksDBHelper() {
        this.statistics = new Statistics();
    }

    public void setCompressionType(CompressionType compressionType) {
        this.compressionType = compressionType;
    }

    public void setBottommostCompressionType(CompressionType bottommostCompressionType) {
        this.bottommostCompressionType = bottommostCompressionType;
    }

    public void setBlockSizeBytes(Integer blockSizeBytes) {
        this.blockSizeBytes = blockSizeBytes;
    }

    public void setOptimizeFiltersForMemory(Boolean optimizeFiltersForMemory) {
            this.optimizeFiltersForMemory = optimizeFiltersForMemory;
    }

    public void setSetCacheIndexAndFilterBlocks(Boolean setCacheIndexAndFilterBlocks) {
        this.setCacheIndexAndFilterBlocks = setCacheIndexAndFilterBlocks;
    }

    public void setPinL0FilterAndIndexBlocksInCache(Boolean pinL0FilterAndIndexBlocksInCache) {
        this.pinL0FilterAndIndexBlocksInCache = pinL0FilterAndIndexBlocksInCache;
    }

    public void setMaxBackgroundJobs(Integer maxBackgroundJobs) {
        this.maxBackgroundJobs = maxBackgroundJobs;
    }

    public void setBytesPerSync(Long bytesPerSync) {
        this.bytesPerSync = bytesPerSync;
    }

    public Statistics getStatistics() {
        return statistics;
    }

    @Override
    protected ColumnFamilyOptions configureRocksDBColumnFamilyOptions() {
        // Start from the "recommended" defaults in RocksDBHelper
        ColumnFamilyOptions options = super.configureRocksDBColumnFamilyOptions();

        if (compressionType != null) {
            options.setCompressionType(compressionType);
        }
        if (bottommostCompressionType != null) {
            options.setBottommostCompressionType(bottommostCompressionType);
        }

        return options;
    }

    @Override
    protected DBOptions configureRocksDBOptions() {
        var options = new Options();

        if (maxBackgroundJobs != null) {
            options.setMaxBackgroundJobs(maxBackgroundJobs);
        } else {
            options.setMaxBackgroundJobs(6);
        }
        if (bytesPerSync != null) {
            options.setBytesPerSync(bytesPerSync);
        } else {
            options.setBytesPerSync(1048576L);
        }

        LOG.debug("compactionPriority {} to {}", options.compactionPriority(), CompactionPriority.MinOverlappingRatio);
        options.setCompactionPriority(CompactionPriority.MinOverlappingRatio);

        var tableOptions = new BlockBasedTableConfig();
        if(blockSizeBytes != null) {
            LOG.debug("blockSize {}", blockSizeBytes);
            tableOptions.setBlockSize(blockSizeBytes);
        } else {
            tableOptions.setBlockSize(16 * 1024); // default
        }

        if (optimizeFiltersForMemory != null) {
            tableOptions.setOptimizeFiltersForMemory(optimizeFiltersForMemory);
        }

        if (setCacheIndexAndFilterBlocks != null) {
            tableOptions.setCacheIndexAndFilterBlocks(setCacheIndexAndFilterBlocks);
        } else {
            tableOptions.setCacheIndexAndFilterBlocks(true); // default
        }
        if(pinL0FilterAndIndexBlocksInCache != null) {
            tableOptions.setPinL0FilterAndIndexBlocksInCache(pinL0FilterAndIndexBlocksInCache);
        } else {
            tableOptions.setPinL0FilterAndIndexBlocksInCache(true); // default
        }

        var newFilterPolicy = new BloomFilter(10.0);
        LOG.debug("filterPolicy {} to {}", tableOptions.filterPolicy(), newFilterPolicy);
        tableOptions.setFilterPolicy(newFilterPolicy);
        LOG.debug("formatVersion {} to {}", tableOptions.formatVersion(), 5);
        tableOptions.setFormatVersion(5);

        options.setTableFormatConfig(tableOptions);

        options.setStatistics(statistics);

        return new DBOptions(options);
    }
}
