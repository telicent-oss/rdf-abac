package io.telicent.jena.abac.core;

import io.telicent.jena.abac.labels.Label;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.system.Txn;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static io.telicent.jena.abac.labels.LabelsStoreRocksDBBenchmark.buildLabelsStoreRocksDB;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
public class DatasetGraphABACBenchmark {

    @Param({"100000"})
    public int datasetSize;

    private DatasetGraph datasetGraph;

    private Quad[] quads;
    private Random random;

    @Setup(Level.Trial)
    public void setup() throws IOException {
        datasetGraph = new DatasetGraphABAC(DatasetGraphFactory.createTxnMem(), "attr=1", buildLabelsStoreRocksDB(), Label.fromText("test"), new AttributesStoreLocal());
        random = new Random(42); // Seed for reproducibility
        quads = new Quad[datasetSize];
        populateDataset();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if(datasetGraph != null){
            datasetGraph.close();
        }
    }

    private void populateDataset() {
        Txn.executeWrite(datasetGraph, () -> {
            for (int i = 0; i < datasetSize; i++) {
                Quad quad = generateQuad(i);
                datasetGraph.add(quad);
                quads[i] = quad;
            }
        });
    }

    private Quad generateQuad(int i) {
        Node graphName = NodeFactory.createURI("http://telicent.io/graph/" + (i % 10)); // 10 graphs
        Node subject = NodeFactory.createURI("http://telicent.io/subject/" + i);
        Node predicate = NodeFactory.createURI("http://telicent.io/predicate/" + (i % 5)); // 5 predicates
        Node object = NodeFactory.createLiteralString("object-" + i);
        return Quad.create(graphName, subject, predicate, object);
    }

    @Benchmark
    public void benchmarkAddQuad() {
        Quad quad = generateQuad(datasetSize + random.nextInt(100)); // Add a new random quad
        Txn.executeWrite(datasetGraph, () -> datasetGraph.add(quad));
    }

    @Benchmark
    public void benchmarkContainsQuad(Blackhole blackhole) {
        Quad quad = quads[random.nextInt(datasetSize)];
        Txn.executeRead(datasetGraph, () -> blackhole.consume(datasetGraph.contains(quad)));
    }

    @Benchmark
    public void benchmarkGetGraph(Blackhole blackhole) {
        Node graphName = NodeFactory.createURI("http://telicent.io/graph/" + random.nextInt(10));
        Txn.executeRead(datasetGraph, () -> blackhole.consume(datasetGraph.getGraph(graphName)));
    }

    @Benchmark
    public void benchmarkContainsTriple(Blackhole blackhole) {
        Quad quad = quads[random.nextInt(datasetSize)];
        Triple triple = quad.asTriple();
        Txn.executeRead(datasetGraph, () -> blackhole.consume(datasetGraph.contains(quad.getGraph(), triple.getSubject(), triple.getPredicate(), triple.getObject())));
    }
}