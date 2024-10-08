package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.ABACTests;
import io.telicent.jena.abac.labels.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Run AbstractTestLabelsStore with for the non-rocks labels index setup.
 * This is for consistency checking.
 */
public class TestLabelsStoreMemGraphRocksDB extends AbstractTestLabelsStoreRocksDB {
    @Override
    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        // The graph-based store does not have a mode
        return Labels.createLabelsStoreMem();
    }

    @Override
    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt, Graph graph) {
        // The graph-based store does not have a mode
        LabelsStore s = Labels.createLabelsStoreMem();
        s.addGraph(graph);
        return s;
    }

    private static String labelsGraph = """
        PREFIX foo: <http://example/>
        PREFIX authz: <http://telicent.io/security#>
        ## No bar:
        [ authz:pattern 'bar:s bar:p1 123' ;  authz:label "allowed" ] .
        """;
    private static Graph BAD_PATTERN = RDFParser.fromString(labelsGraph, Lang.TTL).toGraph();

    @Test
    public void labels_bad_labels_graph() {
        assertThrows(LabelsException.class,
            () -> ABACTests.loggerAtLevel(Labels.LOG, "FATAL",
                () -> createLabelsStore(LabelsStoreRocksDB.LabelMode.Merge, null, BAD_PATTERN))  // warning and error
        );
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labels_bad_labels_graph(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        assertThrows(LabelsException.class,
            () -> ABACTests.loggerAtLevel(Labels.LOG, "FATAL",
                () -> createLabelsStore(labelMode, storeFmt, BAD_PATTERN))  // warning and error
        );
    }

    /**
     * This is parameterized to catch possible future issues, but currently the parameter is ignored
     * @param labelMode ignored
     */
    @Override
    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labels_add_bad_labels_graph(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        String gs = """
            PREFIX : <http://example>
            PREFIX authz: <http://telicent.io/security#>
            [ authz:pattern 'jibberish' ;  authz:label "allowed" ] .
            """;
        Graph addition = RDFParser.fromString(gs, Lang.TTL).toGraph();

        assertThrows(LabelsException.class,
            () -> ABACTests.loggerAtLevel(Labels.LOG, "FATAL", () -> {
                store.addGraph(addition);
                store.labelsForTriples(triple1);
            }));
    }
}
