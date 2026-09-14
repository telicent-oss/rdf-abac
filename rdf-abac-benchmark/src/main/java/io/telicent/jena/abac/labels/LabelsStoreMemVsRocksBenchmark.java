package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import org.rocksdb.RocksDBException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static io.telicent.jena.abac.labels.hashing.HasherUtil.createXX128Hasher;

/**
 * Compare lookup performance between:
 *  - LabelsStoreRocksDB
 *  - LabelsStoreMem
 * on the same dataset and read workload.
 * This will allow us to introduce new and differing Label Stores for comparison
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@SuppressWarnings("deprecation")
public class LabelsStoreMemVsRocksBenchmark {

    public static final Label EDITOR_ROLE_LABEL = Label.fromText("role = 'editor'");
    @Param({ "100000", "1000000"})
    public int tripleCount;

    @Param({"1000000"})
    public int readsPerInvocation;

    private DictionaryLabelStoreRocksDB rocksStore;
    private LabelsStore memStore;

    private Triple[] triples;

    @Setup(Level.Trial)
    public void setup() throws IOException, RocksDBException {
        triples = new Triple[tripleCount];
        for (int i = 0; i < tripleCount; i++) {
            Node s = NodeFactory.createURI("http://example.org/s/" + (i % 10000));
            Node p = NodeFactory.createURI("http://example.org/p/" + (i % 32));
            Node o = NodeFactory.createLiteralString("o-" + i);
            triples[i] = Triple.create(s, p, o);
        }

        // RocksDB store
        File dbDir = Files.createTempDirectory("label-store-comparison").toFile();
        dbDir.deleteOnExit();

        StoreFmt storeFmt = new StoreFmtByHash(createXX128Hasher());
        rocksStore = new DictionaryLabelStoreRocksDB(
                dbDir,
                storeFmt
        );

        memStore = LabelsStoreMem.create();

        for (Triple t : triples) {
            rocksStore.add(t, EDITOR_ROLE_LABEL);
            memStore.add(t, EDITOR_ROLE_LABEL);
        }
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (rocksStore != null) {
            rocksStore.close();
        }
        rocksStore = null;
        memStore = null;
    }

    @Benchmark
    public void rocks_read_hot_hits(Blackhole bh) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Triple t = triples[i % tripleCount];
            Label label = rocksStore.labelForTriple(t);
            bh.consume(label);
        }
    }

    @Benchmark
    public void mem_read_hot_hits(Blackhole bh) {
        for (int i = 0; i < readsPerInvocation; i++) {
            Triple t = triples[i % tripleCount];
            Label label = memStore.labelForTriple(t);
            bh.consume(label);
        }
    }
}
