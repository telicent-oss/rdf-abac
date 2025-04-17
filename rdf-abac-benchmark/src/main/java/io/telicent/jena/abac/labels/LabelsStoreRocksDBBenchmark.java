package io.telicent.jena.abac.labels;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime) // Measures average execution time per operation
@OutputTimeUnit(TimeUnit.MILLISECONDS) // Results in milliseconds
@State(Scope.Thread) // Each thread gets its own instance
public class LabelsStoreRocksDBBenchmark {

    private static final int LABEL_LENGTH = 100;
    private static final int MAX_LABELS = 10;

    private LabelsStoreRocksDB labelsStore;
    @Param({"1000000"})
    private int arraySize;
    private final Random random = new Random();

    private Triple[] randomisedTriples;

    public static LabelsStoreRocksDB buildLabelsStoreRocksDB() throws IOException {
        File dbDir = Files.createTempDirectory("benchmark").toFile();
        dbDir.deleteOnExit();
        return new LabelsStoreRocksDB(new RocksDBHelper(), dbDir, new StoreFmtByString(), LabelsStoreRocksDB.LabelMode.Overwrite, null);
    }

    @Setup(Level.Trial)
    public void setup() throws Exception {
        labelsStore = buildLabelsStoreRocksDB();
        addEntries(arraySize);
        randomiseTriples();
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (labelsStore != null) {
            labelsStore.close();
        }
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
        int numLabels = random.nextInt(MAX_LABELS) + 1; // 1 to MAX_LABELS
        List<Label> labels = new ArrayList<>();
        for (int i = 0; i < numLabels; i++) {
            labels.add(Label.fromText(RandomStringUtils.insecure().nextAlphanumeric(LABEL_LENGTH)));
        }
        return labels;
    }
}
