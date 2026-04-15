package io.telicent.jena.abac;

import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.*;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.QueryExecDatasetBuilder;
import org.apache.jena.system.Txn;
import org.apache.jena.tdb2.TDB2Factory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import static io.telicent.jena.abac.TestAllNamedGraphs.*;

/**
 * Tests that verify {@link AllNamedGraphs} behaves correctly with a TDB2 backed database and doesn't incur undue
 * performance penalties
 */
public class TestAllNamedGraphsTdb2 {

    private File dbDir;

    @BeforeEach
    public void setupTdb() throws IOException {
        this.dbDir = Files.createTempDirectory("tdb2").toFile();
    }

    @AfterEach
    public void teardownTdb() throws IOException {
        FileUtils.deleteDirectory(this.dbDir);
    }

    private static Stream<Arguments> graphSizes() {
        return Stream.of(Arguments.of(100), Arguments.of(10_000), Arguments.of(100_000));
    }

    @ParameterizedTest
    @MethodSource("graphSizes")
    public void givenTdb2Dataset_whenWrappingWithAllNamedGraphs_thenComputationCostIncurredOnce(int numGraphs) {
        // Given
        DatasetGraph dsg = TDB2Factory.connectDataset(dbDir.getAbsolutePath()).asDatasetGraph();
        try {
            addNamedGraphs(dsg, numGraphs);

            // When
            AllNamedGraphs ang = new AllNamedGraphs(dsg);

            // Then
            long start = System.currentTimeMillis();
            Assertions.assertFalse(ang.isEmpty());
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Initial Computation: " + elapsed);
            start = System.currentTimeMillis();
            Assertions.assertEquals(numGraphs, ang.size());
            System.out.println("Subsequent Computation: " + (System.currentTimeMillis() - start));
            Assertions.assertTrue(System.currentTimeMillis() - start < elapsed);
        } finally {
            dsg.close();
        }
    }

    @ParameterizedTest
    @MethodSource("graphSizes")
    public void givenTdb2DatasetWithNoNamedGraphs_whenWrappingWithAllNamedGraphs_thenComputationCostIncurredOnce(
            int numTriples) {
        // Given
        DatasetGraph dsg = TDB2Factory.connectDataset(dbDir.getAbsolutePath()).asDatasetGraph();
        try {
            Txn.executeWrite(dsg, () -> {
                for (int i = 1; i <= numTriples; i++) {
                    dsg.add(Quad.defaultGraphIRI, SUBJECT, PREDICATE,
                            NodeFactory.createLiteralString(Integer.toString(i)));
                }
            });

            // When
            AllNamedGraphs ang = new AllNamedGraphs(dsg);

            // Then
            long start = System.currentTimeMillis();
            Assertions.assertTrue(ang.isEmpty());
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Initial Computation: " + elapsed);
            start = System.currentTimeMillis();
            Assertions.assertEquals(0, ang.size());
            System.out.println("Subsequent Computation: " + (System.currentTimeMillis() - start));
            Assertions.assertTrue(System.currentTimeMillis() - start <= elapsed);
        } finally {
            dsg.close();
        }
    }

    @ParameterizedTest
    @MethodSource("graphSizes")
    public void givenTdb2DatasetWithNamedGraphs_whenWrappingWithAllNamedGraphs_thenQueryingOnlyDefaultGraphIncursNoPerformancePenalty(
            int numGraphs) {
        // Given
        DatasetGraph dsg = TDB2Factory.connectDataset(dbDir.getAbsolutePath()).asDatasetGraph();
        try {
            addNamedGraphs(dsg, numGraphs);
            Txn.executeWrite(dsg, () -> dsg.add(Quad.defaultGraphIRI, SUBJECT, PREDICATE, OBJECT));

            // When
            AllNamedGraphs ang = new AllNamedGraphs(dsg);
            DatasetGraphFilteredView dsgFiltered = new DatasetGraphFilteredView(dsg, null, ang);

            // Then
            Txn.executeRead(dsgFiltered, () -> {
                try (QueryExec qexec = QueryExecDatasetBuilder.create()
                                                              .dataset(dsgFiltered)
                                                              .query("ASK WHERE { ?s ?p ?o }")
                                                              .build()) {
                    long start = System.currentTimeMillis();
                    Assertions.assertTrue(qexec.ask());
                    long elapsed = System.currentTimeMillis() - start;
                    Assertions.assertTrue(elapsed < 1_000);
                    System.out.println("Query took: " + elapsed);
                }
            });
        } finally {
            dsg.close();
        }
    }

    @Test
    public void givenVeryLargeTdb2Dataset_whenWrappingWithAllNamedGraphs_thenComputationCostIncurredOnce() {
        // Given
        DatasetGraph dsg = TDB2Factory.connectDataset(dbDir.getAbsolutePath()).asDatasetGraph();
        try {
            // Generate 10_000_000 triples
            long start = System.currentTimeMillis();
            System.out.println("Generating very large test dataset...");
            Txn.executeWrite(dsg, () -> {
              for (int g = 1; g <= 10_000; g++) {
                  Node graph = NodeFactory.createURI("https://graphs/" + g);
                  for (int t = 1; t <= 1_000; t++) {
                      dsg.add(graph, SUBJECT, PREDICATE, NodeFactory.createLiteralString("object " + t));
                  }
              }
            });
            System.out.format("Generating very large test dataset complete in %,d milliseconds\n", System.currentTimeMillis() - start);

            // When
            AllNamedGraphs ang = new AllNamedGraphs(dsg);

            // Then
            start = System.currentTimeMillis();
            Assertions.assertFalse(ang.isEmpty());
            long elapsed = System.currentTimeMillis() - start;
            System.out.println("Initial Computation: " + elapsed);
            start = System.currentTimeMillis();
            Assertions.assertEquals(10_000, ang.size());
            System.out.println("Subsequent Computation: " + (System.currentTimeMillis() - start));
            Assertions.assertTrue(System.currentTimeMillis() - start < elapsed);
        } finally {
            dsg.close();
        }
    }
}
