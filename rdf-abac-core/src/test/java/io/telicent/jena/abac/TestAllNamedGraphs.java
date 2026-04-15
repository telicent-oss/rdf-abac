package io.telicent.jena.abac;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.DatasetGraphWrapper;
import org.apache.jena.sparql.core.Quad;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Iterator;

public class TestAllNamedGraphs {

    public static final org.apache.jena.graph.Node SUBJECT = NodeFactory.createURI("https://s");
    public static final org.apache.jena.graph.Node PREDICATE = NodeFactory.createURI("https://p");
    public static final org.apache.jena.graph.Node OBJECT = NodeFactory.createLiteralString("object");

    @Test
    public void givenEmptyDataset_whenWrappingWithAllNamedGraphs_thenEmptyReported() {
        // Given
        DatasetGraph dsg = DatasetGraphFactory.empty();

        // When
        AllNamedGraphs ang = new AllNamedGraphs(dsg);

        // Then
        Assertions.assertTrue(ang.isEmpty());
        Assertions.assertEquals(0, ang.size());
    }

    @Test
    public void givenDatasetWithDefaultGraphOnly_whenWrappingWithAllNamedGraphs_thenEmptyReported() {
        // Given
        DatasetGraph dsg = DatasetGraphFactory.create();
        dsg.add(Quad.defaultGraphIRI, SUBJECT, PREDICATE, OBJECT);

        // When
        AllNamedGraphs ang = new AllNamedGraphs(dsg);

        // Then
        Assertions.assertTrue(ang.isEmpty());
        Assertions.assertEquals(0, ang.size());
        Assertions.assertFalse(ang.contains(Quad.defaultGraphIRI));
    }

    private void addNamedGraphs(DatasetGraph dsg, int numGraphs) {
        for (int i = 1; i <= numGraphs; i++) {
            dsg.add(graphName(i), SUBJECT, PREDICATE, OBJECT);
        }
    }

    private void verifyNamedGraphPresent(AllNamedGraphs ang, Node graph) {
        Assertions.assertTrue(ang.contains(graph));
    }

    private static Node graphName(int i) {
        return NodeFactory.createURI("https://graphs/" + i);
    }

    @Test
    public void givenDatasetWithNamedGraphs_whenWrappingWithAllNamedGraphs_thenNamedGraphsListed() {
        // Given
        DatasetGraph dsg = DatasetGraphFactory.create();
        addNamedGraphs(dsg, 5);

        // When
        AllNamedGraphs ang = new AllNamedGraphs(dsg);

        // Then
        Assertions.assertFalse(ang.isEmpty());
        Assertions.assertEquals(5L, ang.size());
        for (int i = 1; i <= 5; i++) {
            verifyNamedGraphPresent(ang, graphName(i));
        }
    }

    @Test
    public void givenDatasetWithSlowNamedGraphComputation_whenWrappingWithAllNamedGraphs_thenComputationCostIncurredOnce() {
        // Given
        DatasetGraph dsg = DatasetGraphFactory.create();
        addNamedGraphs(dsg, 10);

        // When
        AllNamedGraphs ang = new AllNamedGraphs(new DatasetSlowNamedGraphComputation(dsg));

        // Then
        long start = System.currentTimeMillis();
        Assertions.assertFalse(ang.isEmpty());
        Assertions.assertTrue(System.currentTimeMillis() - start > 3_000);
        start = System.currentTimeMillis();
        Assertions.assertEquals(10, ang.size());
        Assertions.assertTrue(System.currentTimeMillis() - start < 3_000);
    }

    private static final class DatasetSlowNamedGraphComputation extends DatasetGraphWrapper {

        public DatasetSlowNamedGraphComputation(DatasetGraph dsg) {
            super(dsg);
        }

        @Override
        public Iterator<Node> listGraphNodes() {
            try {
                Thread.sleep(Duration.ofSeconds(3));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return super.listGraphNodes();
        }
    }
}
