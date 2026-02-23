package io.telicent.jena.abac.labels;

import io.telicent.smart.cache.storage.labels.DictionaryLabelsStore;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmarks comparing RocksDB label store performance with and without
 * a DictionaryLabelsStore for compact label encoding.
 * <p>
 * The dictionary maps label strings to integer IDs, reducing storage size
 * and potentially improving read performance at the cost of an extra
 * lookup/encoding step on writes.
 * <p>
 * Benchmarks:
 *  - read_hits_plain:       100% hits, RocksDB without dictionary
 *  - read_hits_dictionary:  100% hits, RocksDB with dictionary
 *  - read_mixed_plain:      50% hits / 50% misses, RocksDB without dictionary
 *  - read_mixed_dictionary: 50% hits / 50% misses, RocksDB with dictionary
 *  - write_plain:           bulk writes, RocksDB without dictionary
 *  - write_dictionary:      bulk writes, RocksDB with dictionary
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
public class DictionaryLabelsStoreBenchmark extends BenchmarkBase {

    /**
     * Number of SPO triples preloaded into each store.
     */
    @Param({"100000", "1000000"})
    public int tripleCount;

    /**
     * Number of read operations per benchmark invocation.
     * Reported score is "ms per N lookups".
     */
    @Param({"1000000"})
    public int readsPerInvocation;

    /**
     * Number of write operations per benchmark invocation.
     */
    @Param({"100000"})
    public int writesPerInvocation;

    private LabelsStoreRocksDB plainStore;
    private LabelsStoreRocksDB dictionaryStore;

    private Triple[] hitTriples;
    private Triple[] mixedTriples;
    private Triple[] writeTriples;
    private List<Label>[] writeLabels;


    @Setup(Level.Trial)
    public void setUp() throws IOException {
        random = new Random(42L);

        // Plain RocksDB store (no dictionary)
        final File plainDir = Files.createTempDirectory("labels-jmh-plain").toFile();
        plainDir.deleteOnExit();
        final RocksDBHelper plainHelper = new RocksDBHelper();
        final StoreFmt storeFmt = new StoreFmtByString();
        plainStore = new LabelsStoreRocksDB(
                plainHelper,
                plainDir,
                storeFmt,
                LabelsStoreRocksDB.LabelMode.Overwrite,
                null
        );

        // RocksDB store with DictionaryLabelsStore
        final File dictionaryDir = Files.createTempDirectory("labels-jmh-dictionary").toFile();
        dictionaryDir.deleteOnExit();
        final File dictionarySubDir = new File(dictionaryDir, "dictionary");
        dictionarySubDir.mkdirs();
        final RocksDBHelper dictionaryHelper = new RocksDBHelper();
        final DictionaryLabelsStore dictionaryLabelsStore = Labels.createDictionaryLabelsStore(
                dictionarySubDir, Labels.DEFAULT_DICTIONARY_CACHE_SIZE
        );
        dictionaryStore = new LabelsStoreRocksDB(
                dictionaryHelper,
                dictionaryDir,
                storeFmt,
                LabelsStoreRocksDB.LabelMode.Overwrite,
                null,
                dictionaryLabelsStore
        );

        // Pre-generate and load triples into both stores
        hitTriples = new Triple[tripleCount];
        for (int i = 0; i < tripleCount; i++) {
            Triple t = generateDataTriple(i);
            hitTriples[i] = t;

            List<Label> labels = generateRandomLabels();
            plainStore.add(t, labels);
            dictionaryStore.add(t, labels);
        }

        // Prepare mixed workload: 50% hits, 50% misses
        mixedTriples = new Triple[readsPerInvocation];
        for (int i = 0; i < readsPerInvocation; i++) {
            if ((i & 1) == 0) {
                mixedTriples[i] = hitTriples[i % tripleCount];
            } else {
                int idx = i % tripleCount;
                Triple base = hitTriples[idx];
                Node s = base.getSubject();
                Node p = base.getPredicate();
                Node oMiss = NodeFactory.createLiteralString("missing-object-" + idx);
                mixedTriples[i] = Triple.create(s, p, oMiss);
            }
        }

        // Pre-generate write workload (triples that don't exist in the stores yet)
        @SuppressWarnings("unchecked")
        List<Label>[] labelsArr = new List[writesPerInvocation];
        writeLabels = labelsArr;
        writeTriples = new Triple[writesPerInvocation];
        for (int i = 0; i < writesPerInvocation; i++) {
            writeTriples[i] = generateWriteTriple(i);
            writeLabels[i] = generateRandomLabels();
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (plainStore != null) {
            plainStore.close();
        }
        if (dictionaryStore != null) {
            dictionaryStore.close();
        }
        plainStore = null;
        dictionaryStore = null;
        hitTriples = null;
        mixedTriples = null;
        writeTriples = null;
        writeLabels = null;
    }

    // ---- Read benchmarks: 100% hits ----

    @Benchmark
    public void read_hits_plain(Blackhole bh) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Triple t = hitTriples[i % tripleCount];
            List<Label> labels = plainStore.labelsForTriples(t);
            bh.consume(labels);
        }
    }

    @Benchmark
    public void read_hits_dictionary(Blackhole bh) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Triple t = hitTriples[i % tripleCount];
            List<Label> labels = dictionaryStore.labelsForTriples(t);
            bh.consume(labels);
        }
    }

    // ---- Read benchmarks: 50% hits, 50% misses ----

    @Benchmark
    public void read_mixed_plain(Blackhole bh) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Triple t = mixedTriples[i];
            List<Label> labels = plainStore.labelsForTriples(t);
            bh.consume(labels);
        }
    }

    @Benchmark
    public void read_mixed_dictionary(Blackhole bh) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Triple t = mixedTriples[i];
            List<Label> labels = dictionaryStore.labelsForTriples(t);
            bh.consume(labels);
        }
    }

    // ---- Write benchmarks ----

    @Benchmark
    public void write_plain(Blackhole bh) {
        for (int i = 0; i < writesPerInvocation; i++) {
            plainStore.add(writeTriples[i], writeLabels[i]);
            bh.consume(i);
        }
    }

    @Benchmark
    public void write_dictionary(Blackhole bh) {
        for (int i = 0; i < writesPerInvocation; i++) {
            dictionaryStore.add(writeTriples[i], writeLabels[i]);
            bh.consume(i);
        }
    }

    /** Generate unique triples for writing */
    private Triple generateWriteTriple(int i) {
        return generateDataTriple(tripleCount + i);
    }

}
