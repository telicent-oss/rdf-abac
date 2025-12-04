package io.telicent.jena.abac.labels;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.rocksdb.CompressionType;
import org.rocksdb.HistogramType;
import org.rocksdb.Statistics;
import org.rocksdb.TickerType;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static io.telicent.jena.abac.labels.hashing.HasherUtil.obtainHasherFromConfig;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Thread)
public class LabelsStoreRocksDBBenchmark {

    private static final int LABEL_LENGTH = 100;
    private static final int MAX_LABELS = 10;

    private LabelsStoreRocksDB labelsStore;
    private Statistics statistics;
    private BenchmarkRocksDBHelper helper;
    private File dbDir;
    @Param({"1000000"})
    private int arraySize;

    /**
     * Compression for all levels except bottommost.
     *
     * You can add/remove values as you like:
     * - NO_COMPRESSION
     * - SNAPPY_COMPRESSION
     * - LZ4_COMPRESSION
     * - ZSTD_COMPRESSION
     */
//    @Param({"LZ4_COMPRESSION", "ZSTD_COMPRESSION", "NO_COMPRESSION"})
    @Param({"LZ4_COMPRESSION"})
    private String compressionType;
    /**
     * Compression for the bottommost level – often ZSTD is a good choice.
     */
    @Param({"ZSTD_COMPRESSION"})
    private String bottommostCompressionType;

    /**
     * Block size options
     */
//    @Param({"4096", "16384", "65536"})
    private int blockSizeBytes;

//    @Param({"true", "false"})
    private Boolean optimizeFiltersForMemory;
//    @Param({"true", "false"})
    private Boolean setCacheIndexAndFilterBlocks;
//    @Param({"true", "false"})
    private Boolean pinL0FilterAndIndexBlocksInCache;

//    @Param({"xx128", "xx64", "xx32", "sha256", "sha512", "string"})
    private String storageFormat;

    private final Random random = new Random();

    private Triple[] randomisedTriples;

    public static StoreFmt getStoreFmt(String storageFormat) {
        if (storageFormat != null) {
            if (!storageFormat.equalsIgnoreCase("string")) {
                return new StoreFmtByHash(obtainHasherFromConfig(storageFormat));
            }
        }
        return new StoreFmtByString();
    }

    public static LabelsStoreRocksDB buildLabelsStoreRocksDB()
            throws IOException {
        RocksDBHelper helper = new BenchmarkRocksDBHelper();
        File dbDir = Files.createTempDirectory("benchmark").toFile();
        dbDir.deleteOnExit();
        return buildLabelsStoreRocksDB(helper, dbDir);
    }
    public static LabelsStoreRocksDB buildLabelsStoreRocksDB(RocksDBHelper helper, File dbDir)
            throws IOException {
        return buildLabelsStoreRocksDB(helper, dbDir, getStoreFmt(null));
    }
    public static LabelsStoreRocksDB buildLabelsStoreRocksDB(RocksDBHelper helper, File dbDir, StoreFmt storeFmt)
            throws IOException {
        return new LabelsStoreRocksDB(
                helper,
                dbDir,
                storeFmt,
                LabelsStoreRocksDB.LabelMode.Overwrite,
                null
        );
    }


    @Setup(Level.Trial)
    public void setup() throws Exception {
        CompressionType compression = CompressionType.valueOf(compressionType);
        CompressionType bottommost = CompressionType.valueOf(bottommostCompressionType);

        helper = new BenchmarkRocksDBHelper();
        helper.setBlockSizeBytes(blockSizeBytes);
        helper.setCompressionType(compression);
        helper.setBottommostCompressionType(bottommost);
        helper.setOptimizeFiltersForMemory(optimizeFiltersForMemory);
        helper.setPinL0FilterAndIndexBlocksInCache(pinL0FilterAndIndexBlocksInCache);
        helper.setSetCacheIndexAndFilterBlocks(setCacheIndexAndFilterBlocks);


        statistics = helper.getStatistics();

        dbDir = Files.createTempDirectory("benchmark").toFile();
        dbDir.deleteOnExit();

        labelsStore = buildLabelsStoreRocksDB(helper, dbDir, getStoreFmt(storageFormat));

        randomiseTriples();
        addEntries(arraySize);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        dumpRocksDBStats();
        dumpDiskUsage();
        if (labelsStore != null) {
            labelsStore.close();
        }
    }

    private void dumpRocksDBStats() {
        if (statistics == null) {
            return;
        }

        long blockHits  = statistics.getTickerCount(TickerType.BLOCK_CACHE_HIT);
        long blockMiss  = statistics.getTickerCount(TickerType.BLOCK_CACHE_MISS);
        long memHits    = statistics.getTickerCount(TickerType.MEMTABLE_HIT);
        long memMiss    = statistics.getTickerCount(TickerType.MEMTABLE_MISS);
        long bytesRead  = statistics.getTickerCount(TickerType.BYTES_READ);
        long bytesWrite = statistics.getTickerCount(TickerType.BYTES_WRITTEN);

        double cacheHitRate =
                (blockHits + blockMiss) == 0
                        ? 0.0
                        : (double) blockHits / (blockHits + blockMiss);

        String getLatency = statistics.getHistogramString(HistogramType.DB_GET);

        System.out.printf(
                "%nRocksDB stats [compression=%s, bottom=%s]%n" +
                        "  block cache hit-rate: %.2f%n" +
                        "  memtable hits/misses: %d / %d%n" +
                        "  bytes read/written  : %d / %d%n" +
                        "  DB_GET histogram    : %s%n%n",
                compressionType,
                bottommostCompressionType,
                cacheHitRate,
                memHits, memMiss,
                bytesRead, bytesWrite,
                getLatency
        );
    }

    private void dumpDiskUsage() {
        if (dbDir == null) {
            return;
        }

        long bytesOnDisk = 0L;

        try (Stream<Path> paths = Files.walk(dbDir.toPath())) {
            bytesOnDisk = paths
                    .filter(Files::isRegularFile)
                    .mapToLong(p -> {
                        try {
                            return Files.size(p);
                        } catch (IOException e) {
                            return 0L;
                        }
                    })
                    .sum();
        } catch (IOException e) {
            System.err.printf("Failed to compute disk usage for %s: %s%n",
                    dbDir, e.getMessage());
            return;
        }

        double mb = bytesOnDisk / (1024.0 * 1024.0);

        System.out.printf(
                "%nRocksDB disk usage [compression=%s, bottom=%s]%n" +
                        "  directory: %s%n" +
                        "  size     : %d bytes (%.2f MB)%n%n",
                compressionType,
                bottommostCompressionType,
                dbDir.getAbsolutePath(),
                bytesOnDisk,
                mb
        );
    }

    /**
     * Make a million fetches for data that has been added to the Rocks DB Label Store
     */
    @Benchmark
    public void test_labelFetch(Blackhole blackhole) {
        for (int i = 0; i < arraySize; i++) {
            List<Label> labels = labelsStore.labelsForTriples(randomisedTriples[i]);
            blackhole.consume(labels);
        }
    }

    /**
     * Generate randomised triples
     */
    private void randomiseTriples() {
        randomisedTriples = new Triple[arraySize];
        for (int i = 0; i < arraySize; i++) {
            randomisedTriples[i] = generateRandomTriple();
        }
    }

    /**
     * Adds triples to the Rocks DB
     * @param amount the number of entries
     */
    private void addEntries(int amount) {
        for (int i = 0; i < amount; i++) {
            labelsStore.add(generateRandomTriple(), generateRandomLabels());
        }
    }

    private Triple generateRandomTriple() {
        int subjectIndex = random.nextInt(arraySize * 2); // More variety
        int predicateIndex = random.nextInt(10);
        int objectIndex = random.nextInt(arraySize * 2);

        return Triple.create(
                NodeFactory.createURI("subject-" + subjectIndex),
                NodeFactory.createURI("predicate-" + predicateIndex),
                NodeFactory.createLiteralString("object-" + objectIndex)
        );
    }

    private List<Label> generateRandomLabels() {
        int numLabels = random.nextInt(MAX_LABELS) + 1;
        List<Label> labels = new ArrayList<>();
        for (int i = 0; i < numLabels; i++) {
            labels.add(Label.fromText(RandomStringUtils.insecure().nextAlphanumeric(LABEL_LENGTH)));
        }
        return labels;
    }
}
