package io.telicent.jena.abac.labels;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.graph.GraphFactory;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.telicent.jena.abac.labels.LabelsStoreRocksDBBenchmark.buildLabelsStoreRocksDB;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
public class TurtleDirectoryLabelsRocksDBBenchmark {

    /**
     * Directory containing .ttl label files (searched recursively).
     *
     * Override with:
     *   -pttlDir=/path/to/dir
     */
    @Param({"./rdf-abac-core/src/test/files/dataset/"})
    public String ttlDir;

    private List<Path> ttlFiles;

    private final Random random = new Random();

    private LabelsStoreRocksDB labelsStore;

    private static final int LABEL_LENGTH = 100;

    @Setup(Level.Trial)
    public void scanDirectory() throws IOException {
        Path root = Paths.get(ttlDir);
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Not a directory: " + root.toAbsolutePath());
        }

        try (var stream = Files.walk(root)) {
            ttlFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".ttl"))
                    .collect(Collectors.toList());
        }

        if (ttlFiles.isEmpty()) {
            throw new IllegalStateException("No .ttl files found under " + root.toAbsolutePath());
        }
    }

    @Setup(Level.Iteration)
    public void openStore() throws IOException {
        labelsStore = buildLabelsStoreRocksDB();
    }

    @TearDown(Level.Iteration)
    public void closeStore() {
        if (labelsStore != null) {
            labelsStore.close();
        }
    }

    @Benchmark
    public void loadDirectoryIntoRocksDBLabelStore(Blackhole blackhole) {
        List<Label> labels = generateRandomLabels(ttlFiles.size());
        AtomicInteger index = new AtomicInteger();
        for (Path ttl : ttlFiles) {
            try {
                Graph graph = GraphFactory.createDefaultGraph();
                RDFDataMgr.read(graph, ttl.toString());
                graph.find(Node.ANY, Node.ANY, Node.ANY).forEachRemaining(triple -> {
                    // This bypasses authz:pattern completely and writes directly to RocksDB
                    labelsStore.add(triple, labels.get(index.get()));
                });
                index.getAndIncrement();
            } catch (Exception ex) {
                System.out.println("Error loading " + ttl.toAbsolutePath() + " because " + ex.getMessage());
                blackhole.consume(ex);
            }
        }
        blackhole.consume(labelsStore);
    }

    private List<Label> generateRandomLabels(int numLabels) {
        List<Label> labels = new ArrayList<>();
        for (int i = 0; i < numLabels; i++) {
            labels.add(Label.fromText(RandomStringUtils.insecure().nextAlphanumeric(LABEL_LENGTH)));
        }
        return labels;
    }
}
