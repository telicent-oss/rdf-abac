package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.labels.*;
import io.telicent.jena.abac.labels.hashing.Hasher;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.platform.play.PlayFiles;
import org.openjdk.jmh.annotations.*;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import static io.telicent.jena.abac.labels.Labels.rocks;
import static io.telicent.jena.abac.labels.LabelsStoreRocksDB.LabelMode.Overwrite;

/**
 * JMH Class to run the various hash functions that are available
 */
// Benchmark mode and time unit
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)  // Scope of the state
public class PerformanceTestHashers {

    // JMH Param - the key for the hasher
    @Param({
            "city64",
            "farm64",
            "farmna64",
            "farmuo64",
            "metro64",
            "murmur64",
            "murmur128",
            "sha256",
            "sha512",
            "sip24",
            "wy3",
            "xx32",
            "xx64",
            "xx128"
    })

    private String hasherKey;
    private Hasher hasher;
    private LabelsStore labelsStore;
    private File dbDir;

    @Setup(Level.Trial)
    public void setUp() throws IOException, RocksDBException {
        hasher = HasherUtil.hasherMap.get(hasherKey).get();
        dbDir = Files.createTempDirectory("tmp" + hasherKey).toFile();
        labelsStore = Labels.createLabelsStoreRocksDB(dbDir, Overwrite, null, new StoreFmtByHash(hasher));
    }

    @Benchmark
    public void benchmarkHasher() {
        File files = new File("rdf-abac-core/src/test/files/starwars/content");
        PlayFiles.action(files.getAbsolutePath(),
                message -> LabelsLoadingConsumer.consume(labelsStore, message, null),
                headers -> headers.put(SysABAC.hSecurityLabel, "security=unknowndefault"));
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (labelsStore instanceof LabelsStoreRocksDB rocksDB) {
            rocksDB.close();
        }
        rocks.clear();
        dbDir.delete();
        dbDir = null;
    }

    // Main method to run the benchmark - takes about 2 hours
    public static void main(String[] args) throws Exception {
        org.openjdk.jmh.Main.main(args);
    }
}