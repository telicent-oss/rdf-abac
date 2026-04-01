package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.BenchmarkUtils;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Quad;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Baseline JMH benchmarks for the RocksDB-based label store. These are intended to be *stable* metrics we can compare
 * across time.  This is the same benchmark as in {@link LabelsStoreRocksDBBaselineBenchmark} but adapted for the modern
 * dictionary store so it generates quads instead of triples to populate the label store.  However, the basic
 * deterministic method of how those quads are generated is equivalent to how the triples are generated in the original
 * benchmark so numbers are comparable.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class LabelsStoreModernRocksDBBaselineBenchmark {

    /**
     * Number of quads to load into the label store. This determines both DB size and the size of the "hot" read set.
     */
    @Param({ "100000", "1000000" })
    public int quadCount;

    /**
     * Number of read operations performed per benchmark invocation. This amortizes JMH overhead and gives stable timing
     * for "bulk" reads. The reported score will be "ms per invocation", not per single lookup. So 1000000 here means
     * "ms per 1M lookups".
     */
    @Param({ "1000000" })
    public int readsPerInvocation;

    private DictionaryLabelStoreRocksDB labelsStore;

    private Quad[] hitQuads;
    private Quad[] mixedQuads;

    private Random random;

    private static final int GRAPH_CARDINALITY = 100;
    private static final int SUBJECT_CARDINALITY = 10_000;
    private static final int PREDICATE_CARDINALITY = 32;
    private static final int MAX_MULTIPLIER = 8;
    private static final int LABEL_TEXT_LENGTH = 32;

    @Setup(Level.Trial)
    public void setUp() throws IOException, RocksDBException {
        random = new Random(42L);

        File dbDir = Files.createTempDirectory("labels-jmh-baseline").toFile();
        dbDir.deleteOnExit();

        StoreFmt storeFmt = new StoreFmtByHash(HasherUtil.createXX128Hasher());
        labelsStore = new DictionaryLabelStoreRocksDB(dbDir, storeFmt);

        hitQuads = new Quad[quadCount];
        for (int i = 0; i < quadCount; i++) {
            Quad q = generateDataQuad(i);
            hitQuads[i] = q;

            Label labels = generateRandomLabels();
            labelsStore.add(q, labels);
        }

        // Prepare a mixed workload: 50% existing keys, 50% misses
        mixedQuads = new Quad[readsPerInvocation];
        for (int i = 0; i < readsPerInvocation; i++) {
            if ((i & 1) == 0) {
                // Hit: pick a triple from the populated set
                mixedQuads[i] = hitQuads[i % quadCount];
            } else {
                // Miss: same subject+predicate, but object that was never stored
                int idx = i % quadCount;
                Quad base = hitQuads[idx];
                Node g = base.getGraph();
                Node s = base.getSubject();
                Node p = base.getPredicate();
                Node oMiss = NodeFactory.createLiteralString("missing-object-" + idx);
                mixedQuads[i] = Quad.create(g, s, p, oMiss);
            }
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (labelsStore != null) {
            labelsStore.close();
        }
        labelsStore = null;
        hitQuads = null;
        mixedQuads = null;
    }

    /**
     * 100% hits on existing keys. This is a "best case" hot-read metric: - exercise triple normalization, key encoding,
     * RocksDB lookup, and label decoding - no misses, no pattern lookups
     */
    @Benchmark
    public void read_hot_hits(Blackhole blackhole) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Quad q = hitQuads[i % quadCount];
            Label label = labelsStore.labelForQuad(q);
            blackhole.consume(label);
        }
    }

    /**
     * 50% hits, 50% misses. Same as read_hot_hits, but half of the lookups deliberately miss. This gives you a sense
     * of: - branch behaviour for miss path - any extra work done when nothing is found
     */
    @Benchmark
    public void read_hot_mixed(Blackhole blackhole) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Quad q = mixedQuads[i];
            Label labels = labelsStore.labelForQuad(q);
            blackhole.consume(labels);
        }
    }

    /**
     * Generate a reproducible quad for index {@code i}. The pattern ensures: - many distinct objects - repeated
     * graphs/subjects/predicates (more realistic index behaviour)
     */
    private Quad generateDataQuad(int i) {
        int gIndex = i % GRAPH_CARDINALITY;
        int sIndex = i % SUBJECT_CARDINALITY;
        int pIndex = i % PREDICATE_CARDINALITY;
        Node g = NodeFactory.createURI("https://example.org/g/" + gIndex);
        Node s = NodeFactory.createURI("https://example.org/s/" + sIndex);
        Node p = NodeFactory.createURI("https://example.org/p/" + pIndex);
        Node o = NodeFactory.createLiteralString("o-" + i);
        return Quad.create(g, s, p, o);
    }

    /**
     * Generate a random label string
     */
    private Label generateRandomLabels() {
        int multiplier = 1 + random.nextInt(MAX_MULTIPLIER);
        return Label.fromText(RandomStringUtils.insecure().nextAlphanumeric(multiplier * LABEL_TEXT_LENGTH));
    }

    public static void main(String[] args) {
        BenchmarkUtils.run(LabelsStoreModernRocksDBBaselineBenchmark.class);
    }
}
