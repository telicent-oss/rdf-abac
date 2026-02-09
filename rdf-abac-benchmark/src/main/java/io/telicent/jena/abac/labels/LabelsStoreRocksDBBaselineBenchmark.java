package io.telicent.jena.abac.labels;

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
 * Baseline JMH benchmarks for the RocksDB-based label store.
 * These are intended to be *stable* metrics we can compare across time.
 *  - read_hot_hits:   100% hits on existing triples
 *  - read_hot_mixed:  50% hits, 50% misses
 * They:
 *  - Build a RocksDB label store in a temp directory
 *  - Pre-populate it with a configurable number of triples
 *  - Pre-generate the read workloads so the benchmark body does minimal work
 *  It's a starting point and an approach we can apply to all appropriate repos.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class LabelsStoreRocksDBBaselineBenchmark extends BenchmarkBase {

    /**
     * Number of SPO triples to load into the label store.
     * This determines both DB size and the size of the "hot" read set.
     */
    @Param({"100000", "1000000"})
    public int tripleCount;

    /**
     * Number of read operations performed per benchmark invocation.
     * This amortizes JMH overhead and gives stable timing for "bulk" reads.
     * The reported score will be "ms per invocation", not per single lookup.
     * So 1000000 here means "ms per 1M lookups".
     */
    @Param({"1000000"})
    public int readsPerInvocation;

    private LabelsStoreRocksDB labelsStore;
    private File dbDir;

    private Triple[] hitTriples;
    private Triple[] mixedTriples;

    @Setup(Level.Trial)
    public void setUp() throws IOException {
        random = new Random(42L);

        dbDir = Files.createTempDirectory("labels-jmh-baseline").toFile();
        dbDir.deleteOnExit();

        RocksDBHelper helper = new RocksDBHelper();
        StoreFmt storeFmt = new StoreFmtByString();
        labelsStore = new LabelsStoreRocksDB(
                helper,
                dbDir,
                storeFmt,
                LabelsStoreRocksDB.LabelMode.Overwrite,
                null
        );

        hitTriples = new Triple[tripleCount];
        for (int i = 0; i < tripleCount; i++) {
            Triple t = generateDataTriple(i);
            hitTriples[i] = t;

            List<Label> labels = generateRandomLabels();
            labelsStore.add(t, labels);
        }

        // Prepare a mixed workload: 50% existing keys, 50% misses
        mixedTriples = new Triple[readsPerInvocation];
        for (int i = 0; i < readsPerInvocation; i++) {
            if ((i & 1) == 0) {
                // Hit: pick a triple from the populated set
                mixedTriples[i] = hitTriples[i % tripleCount];
            } else {
                // Miss: same subject+predicate, but object that was never stored
                int idx = i % tripleCount;
                Triple base = hitTriples[idx];
                Node s = base.getSubject();
                Node p = base.getPredicate();
                Node oMiss = NodeFactory.createLiteralString(
                        "missing-object-" + idx
                );
                mixedTriples[i] = Triple.create(s, p, oMiss);
            }
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (labelsStore != null) {
            labelsStore.close();
        }
        labelsStore = null;
        hitTriples = null;
        mixedTriples = null;
    }

    /**
     * 100% hits on existing keys.
     * This is a "best case" hot-read metric:
     *  - exercise triple normalization, key encoding,
     *    RocksDB lookup, and label decoding
     *  - no misses, no pattern lookups
     */
    @Benchmark
    public void read_hot_hits(Blackhole blackhole) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Triple t = hitTriples[i % tripleCount];
            List<Label> labels = labelsStore.labelsForTriples(t);
            blackhole.consume(labels);
        }
    }

    /**
     * 50% hits, 50% misses.
     * Same as read_hot_hits, but half of the lookups deliberately miss.
     * This gives you a sense of:
     *  - branch behaviour for miss path
     *  - any extra work done when nothing is found
     */
    @Benchmark
    public void read_hot_mixed(Blackhole blackhole) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Triple t = mixedTriples[i];
            List<Label> labels = labelsStore.labelsForTriples(t);
            blackhole.consume(labels);
        }
    }

}
