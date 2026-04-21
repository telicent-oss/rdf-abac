package io.telicent.jena.abac;

import org.apache.jena.datatypes.xsd.XSDDatatype;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.*;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Iterator;

public class TestAllNamedGraphs {
    public static final org.apache.jena.graph.Node SUBJECT = NodeFactory.createURI("https://s");
    public static final org.apache.jena.graph.Node PREDICATE = NodeFactory.createURI("https://p");
    public static final org.apache.jena.graph.Node OBJECT = NodeFactory.createLiteralString("object");

    public static void addNamedGraphs(DatasetGraph dsg, int numGraphs) {
        Txn.executeWrite(dsg, () -> {
            for (int i = 1; i <= numGraphs; i++) {
                dsg.add(graphName(i), SUBJECT, PREDICATE, OBJECT);
            }
        });
    }

    public static void addNamedGraphsUniqueTriples(DatasetGraph dsg, int numGraphs) {
        Txn.executeWrite(dsg, () -> {
            for (int i = 1; i <= numGraphs; i++) {
                dsg.add(graphName(i), SUBJECT, PREDICATE,
                        NodeFactory.createLiteralDT(Integer.toString(i), XSDDatatype.XSDinteger));
            }
        });
    }

    public static void verifyNamedGraphPresent(AllNamedGraphs ang, Node graph) {
        Assertions.assertTrue(ang.contains(graph));
    }

    public static Node graphName(int i) {
        return NodeFactory.createURI("https://graphs/" + i);
    }

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
        long elapsed = System.currentTimeMillis() - start;
        Assertions.assertTrue(elapsed >= 3_000);
        start = System.currentTimeMillis();
        Assertions.assertEquals(10, ang.size());
        elapsed = System.currentTimeMillis() - start;
        Assertions.assertTrue(elapsed < 3_000);
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

    @Test
    public void givenFilteredDatasetViewWithAllNamedGraphsHavingSameTriple_whenAccessingUnionGraph_thenSingleTripleReturned() {
        // Given
        DatasetGraph dsgBase = DatasetGraphFactory.create();
        addNamedGraphs(dsgBase, 100);
        DatasetGraphFilteredView dsgFiltered = new DatasetGraphFilteredView(dsgBase, null, new AllNamedGraphs(dsgBase));

        // When
        Graph union = dsgFiltered.getUnionGraph();

        // Then
        Assertions.assertEquals(1, union.size());
    }

    @Test
    public void givenFilteredDatasetViewWithAllNamedGraphsHavingDifferentTriples_whenAccessingUnionGraph_thenAllTriplesReturned() {
        // Given
        DatasetGraph dsgBase = DatasetGraphFactory.create();
        addNamedGraphsUniqueTriples(dsgBase, 100);
        DatasetGraphFilteredView dsgFiltered = new DatasetGraphFilteredView(dsgBase, null, new AllNamedGraphs(dsgBase));

        // When
        Graph union = dsgFiltered.getUnionGraph();

        // Then
        Assertions.assertEquals(100, union.size());
    }

    @Test
    public void givenFilteredDatasetViewWithAllNamedGraphsAndNegativeQuadFilter_whenAccessingUnionGraph_thenNoTriplesReturned() {
        // Given
        DatasetGraph dsgBase = DatasetGraphFactory.create();
        addNamedGraphsUniqueTriples(dsgBase, 50);
        DatasetGraphFilteredView dsgFiltered =
                new DatasetGraphFilteredView(dsgBase, q -> false, new AllNamedGraphs(dsgBase));

        // When
        Graph union = dsgFiltered.getUnionGraph();

        // Then
        Assertions.assertEquals(0, union.size());
    }
}
