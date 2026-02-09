package io.telicent.jena.abac.fuseki;


import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.LabelsStore;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.servlets.ActionErrorException;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.riot.RiotException;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sys.JenaSystem;
import org.eclipse.jetty.ee11.servlet.ServletContextHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static io.telicent.jena.abac.fuseki.LabelledDataLoader.ingestData;
import static io.telicent.jena.abac.fuseki.TestServerABAC.server;
import static org.apache.jena.fuseki.system.ActionCategory.ACTION;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestLabelledDataLoader {
    static {
        JenaSystem.init();
    }
    private static final HttpServletRequest MOCK_REQUEST = mock(HttpServletRequest.class);
    private static final HttpServletResponse MOCK_RESPONSE = mock(HttpServletResponse.class);
    private static final Logger LOGGER = mock(Logger.class);
    private static final ServletContextHandler SERVLET_CONTEXT_HANDLER = new ServletContextHandler();
    private static final ServletContext SERVLET_CONTEXT = SERVLET_CONTEXT_HANDLER.getServletContext();

    private static final int INITIAL_GRAPH_SIZE = 2;
    private static final int INITIAL_LABEL_SIZE = 2;

    private static final String TTL_FORMAT = "text/turtle";
    private static final String NQ_FORMAT = "application/n-quads";

    private static final String TTL_UNLABELED_DATA = """
            PREFIX : <http://example/>
            :s :p2 123 .
            :s :p2 456 .
            """;

    private static final String TTL_UNLABELLED_BLANK_NODE_DATA = """
            PREFIX : <http://example/>
            [] :p2 123 .
            [] :p2 456 .
            """;

    private static final String TTL_LABELLED_BLANK_NODE_DATA = """
            PREFIX : <http://example/>
            _:s :p2 123 .
            _:s :p2 456 .
            """;


    private static final String NQ_UNLABELLED_DATA = """
            <http://example/s> <http://example/p2> "123" .
            <http://example/s> <http://example/p2> "456" .
            """;

    private static final String NQ_LABELLED_BLANK_NODE_DATA = """
            _:s <http://example/p2> "123" .
            _:s <http://example/p2> "456" .
            """;


    private static final String NQ_NAMED_GRAPH_DATA = """
            <http://example/s> <http://example/p2> "123" <http://example/> .
            <http://example/s> <http://example/p2> "456" <http://example/> .
            """;


    @BeforeEach
    public void setupTest() {
        when(MOCK_REQUEST.getServletContext()).thenReturn(SERVLET_CONTEXT);
    }

    @AfterEach
    public void teardown() {
        reset(MOCK_REQUEST, MOCK_RESPONSE, LOGGER);
    }

    /**
     * Refactoring of test code - starts server (which loads 2 triples each with label "default").
     *
     * @param dataToIngest representation of the data to ingest
     * @param dataToIngestFormat format of data i.e. turtle or n-quad
     * @param defaultLabels incoming default security labels to apply to data
     * @param expectedTripleCount the number of triples to be processed from the data
     * @param expectedQuadCount the number of quads to be processed from the data
     * @param expectedGraphIncrease the number of additional entries in the underlying graph afterward
     * @param expectedLabelStoreIncrease the number of additional entries in the label store afterward
     */
    private void test_ingestData_implementation(String dataToIngest, String dataToIngestFormat, List<String> defaultLabels,
                                                int expectedTripleCount, int expectedQuadCount, int expectedGraphIncrease, int expectedLabelStoreIncrease) {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;
        try (LabelsStore labelsStore = datasetGraphABAC.labelsStore()) {

            when(MOCK_REQUEST.getContentType()).thenReturn(dataToIngestFormat);
            TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(dataToIngest.getBytes()));
            when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

            ServletOutputStream outputStream = mock(ServletOutputStream.class);
            when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

            // Graph initially populated with 2 entries
            assertEquals(INITIAL_GRAPH_SIZE, datasetGraphABAC.getDefaultGraph().size());
            // Label Store (x 2 in size) when converted to Graph
            assertEquals(INITIAL_LABEL_SIZE, labelsStore.asGraph().size()/2);
            // when
            LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, defaultLabels);
            // then
            assertNotNull(results);
            assertEquals(expectedTripleCount, results.tripleCount(), "triple count does not match");
            assertEquals(expectedQuadCount, results.quadCount(), "quad count does not match");
            assertEquals(INITIAL_GRAPH_SIZE + expectedGraphIncrease, datasetGraphABAC.getDefaultGraph().size(), "Graph increase does not match");
            assertEquals(INITIAL_LABEL_SIZE + expectedLabelStoreIncrease, labelsStore.asGraph().size()/2, "Label store increase does not match");
        } catch (Exception e) {
            fail(e);
        }
    }

    /**
     * Loads a TTL of two unlabelled triples but no labels are provided.
     * The 2 new triples are added to the graph.
     * The default label of the DSG will be applied in absentia.
     */
    @Test
    public void test_ingestData_unlabelledTriples_noDefault() {
        List<String> defaultLabels = List.of(); // No default security labels provided
        int expectedTripleCount = 2; // 2 triples will be processed
        int expectedQuadCount = 0; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 0; // 0 new labels
        test_ingestData_implementation(TTL_UNLABELED_DATA, TTL_FORMAT, defaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads a TTL of two unlabelled triples but no labels are provided.
     * The 2 new triples are added to the graph.
     * The default label of the DSG will be applied in absentia.
     */
    @Test
    public void test_ingestData_unlabelledTriples_nullDefault() {
        int expectedTripleCount = 2; // 2 triples will be processed
        int expectedQuadCount = 0; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 0; // 0 new labels
        test_ingestData_implementation(TTL_UNLABELED_DATA, TTL_FORMAT, null, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads a TTL of two unlabelled triples and a label matching the existing DSG default is provided.
     * The 2 new triples are added to the graph.
     * Given the provided label matches the DGS default, no label is needed as it will be applied in absentia.
     */
    @Test
    public void test_ingestData_unlabelledTriples_defaultMatch() {
        List<String> matchingDefaultLabels = List.of("default"); // matches the default label of DSG
        int expectedTripleCount = 2; // 2 triples will be processed
        int expectedQuadCount = 0; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 0; // 0 new labels
        test_ingestData_implementation(TTL_UNLABELED_DATA, TTL_FORMAT, matchingDefaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads a TTL of two unlabelled triples with a label provided that differs from the DSG default.
     * The 2 new triples are added to the graph as are the two new labels.
     */
    @Test
    public void test_ingestData_unlabelledTriples_differentDefaultLabel() {
        List<String> differentDefaultLabels = List.of("different"); // does not match the default label of DSG ("default")
        int expectedTripleCount = 2; // 2 triples will be processed
        int expectedQuadCount = 0; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 2; // 2 new labels
        test_ingestData_implementation(TTL_UNLABELED_DATA, TTL_FORMAT, differentDefaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads a TTL of two unlabelled blank node triples with a label matching the existing DSG default.
     * The 2 new triples are added to the graph but no new labels.
     */
    @Test
    public void test_ingestData_unlabelledBlankNodeTriples_withDefaultLabel() {
        List<String> matchingDefaultLabels = List.of("default"); // matches the default label of DSG ("default")
        int expectedTripleCount = 2; // 2 triples will be processed
        int expectedQuadCount = 0; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 0; // No new labels
        test_ingestData_implementation(TTL_UNLABELLED_BLANK_NODE_DATA, TTL_FORMAT, matchingDefaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads a TTL of two unlabelled blank node triples with a label that doesn't match the existing DSG default.
     * The 2 new triples are added to the graph as are the two new labels.
     */
    @Test
    public void test_ingestData_unlabelledBlankNodeTriples_withDifferentLabel() {
        List<String> differentDefaultLabels = List.of("different"); // does not match the default label of DSG ("default")
        int expectedTripleCount = 2; // 2 triples will be processed
        int expectedQuadCount = 0; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 2; // 2 new labels
        test_ingestData_implementation(TTL_UNLABELLED_BLANK_NODE_DATA, TTL_FORMAT, differentDefaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads a TTL of two labelled blank node triples with a label matching the existing DSG default.
     * The 2 new triples are added to the graph but no new labels.
     */
    @Test
    public void test_ingestData_labelledBlankNodeTriples_withDefaultLabel() {
        List<String> matchingDefaultLabels = List.of("default"); // matches the default label of DSG ("default")
        int expectedTripleCount = 2; // 2 triples will be processed
        int expectedQuadCount = 0; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 0; // No new labels
        test_ingestData_implementation(TTL_LABELLED_BLANK_NODE_DATA, TTL_FORMAT, matchingDefaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads a TTL of two labelled blank node triples with a label that doesn't match the existing DSG default.
     * The 2 new triples are added to the graph as are the two new labels.
     */
    @Test
    public void test_ingestData_labelledBlankNodeTriples_withDifferentLabel() {
        List<String> differentDefaultLabels = List.of("different"); // does not match the default label of DSG ("default")
        int expectedTripleCount = 2; // 2 triples will be processed
        int expectedQuadCount = 0; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 2; // 2 new labels
        test_ingestData_implementation(TTL_LABELLED_BLANK_NODE_DATA, TTL_FORMAT, differentDefaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads an NQ of two unlabelled quads but no labels are provided.
     * The 2 new triples are added to the graph.
     * The default label of the DSG will be applied in absentia.
     */
    @Test
    public void test_ingestData_unlabelledQuads_noDefault() {
        List<String> defaultLabels = List.of(); // No default security labels provided
        int expectedTripleCount = 0; // 2 triples will be processed
        int expectedQuadCount = 2; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 0; // 0 new labels
        test_ingestData_implementation(NQ_UNLABELLED_DATA, NQ_FORMAT, defaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads an NQ of two unlabelled quads but no labels are provided.
     * The 2 new triples are added to the graph.
     * The default label of the DSG will be applied in absentia.
     */
    @Test
    public void test_ingestData_unlabelledQuads_nullDefault() {
        int expectedTripleCount = 0; // 2 triples will be processed
        int expectedQuadCount = 2; // no quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 0; // 0 new labels
        test_ingestData_implementation(NQ_UNLABELLED_DATA, NQ_FORMAT, null, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads an NQ of two unlabelled quads and a label matching the existing DSG default is provided.
     * The 2 quads are processed and 2 new triples are added to the graph.
     * Given the provided label matches the DGS default, no label is needed as it will be applied in absentia.
     */
    @Test
    public void test_ingestData_unlabelledQuads_defaultLabelMatch() {
        List<String> defaultLabels = List.of("default"); // No default security labels provided
        int expectedTripleCount = 0; // no triples will be processed
        int expectedQuadCount = 2; // 2 quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 0; // 0 new labels
        test_ingestData_implementation(NQ_UNLABELLED_DATA, NQ_FORMAT, defaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads an NQ of two unlabelled quads and a label that does not match the existing DSG default.
     * The 2 quads are processed and 2 new triples are added to the graph.
     * 2 new labels are added too.
     */
    @Test
    public void test_ingestData_unlabelledQuads_differentLabelMatch() {
        List<String> defaultLabels = List.of("different"); // No default security labels provided
        int expectedTripleCount = 0; // no triples will be processed
        int expectedQuadCount = 2; // 2 quads to processed
        int expectedGraphIncrease = 2; // new 2 triples
        int expectedLabelStoreIncrease = 2; // 2 new labels
        test_ingestData_implementation(NQ_UNLABELLED_DATA, NQ_FORMAT, defaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads an NQ of two unlabelled quads and a label matching the existing DSG default.
     * The 2 quads are processed and 2 new triples are added to the graph but not to the default graph.
     * No labels are added as the named graph is not (http://telicent.io/security#labels) in the correct form.
     */
    @Test
    public void test_ingestData_namedGraphQuads_defaultLabelMatch() {
        List<String> defaultLabels = List.of("default"); // No default security labels provided
        int expectedTripleCount = 0; // no triples will be processed
        int expectedQuadCount = 2; // 2 quads to processed
        int expectedGraphIncrease = 0; // no new triples (in default graph)
        int expectedLabelStoreIncrease = 0; // 0 new labels
        test_ingestData_implementation(NQ_NAMED_GRAPH_DATA, NQ_FORMAT, defaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads an NQ of two unlabelled quads and a label that does not match the existing DSG default.
     * The 2 quads are processed and 2 new triples are added to the graph but not to the default graph.
     * No labels are added as the named graph is not (http://telicent.io/security#labels) in the correct form.
     */
    @Test
    public void test_ingestData_namedGraphQuads_differentLabelMatch() {
        List<String> defaultLabels = List.of("different"); // No default security labels provided
        int expectedTripleCount = 0; // no triples will be processed
        int expectedQuadCount = 2; // 2 quads to processed
        int expectedGraphIncrease = 0; // no new triples (in default graph)
        int expectedLabelStoreIncrease = 0; // 0 new labels
        test_ingestData_implementation(NQ_NAMED_GRAPH_DATA, NQ_FORMAT, defaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }


    /**
     * Loads an NQ of two labelled blank node quads and a label that matches the existing DSG default.
     * The 2 quads are processed and 2 new triples are added to the (default) graph.
     * No labels are added as the named graph is not (http://telicent.io/security#labels) in the correct form.
     */
    @Test
    public void test_ingestData_labelledBlankNodeQuad_defaultLabelMatch() {
        List<String> defaultLabels = List.of("default"); // No default security labels provided
        int expectedTripleCount = 0; // no triples will be processed
        int expectedQuadCount = 2; // 2 quads to processed
        int expectedGraphIncrease = 2; // 2 new triples (in default graph)
        int expectedLabelStoreIncrease = 0; // 0 new labels
        test_ingestData_implementation(NQ_LABELLED_BLANK_NODE_DATA, NQ_FORMAT, defaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Loads an NQ of two labelled blank node quads and a label that matches the existing DSG default.
     * The 2 quads are processed and 2 new triples are added to the (default) graph.
     * 2 new labels are added due to the different label that is provided.
     */
    @Test
    public void test_ingestData_labelledBlankNodeQuad_differentLabelMatch() {
        List<String> defaultLabels = List.of("different"); // No default security labels provided
        int expectedTripleCount = 0; // no triples will be processed
        int expectedQuadCount = 2; // 2 quads to processed
        int expectedGraphIncrease = 2; // no new triples (in default graph)
        int expectedLabelStoreIncrease = 2; // 2 new labels
        test_ingestData_implementation(NQ_LABELLED_BLANK_NODE_DATA, NQ_FORMAT, defaultLabels, expectedTripleCount, expectedQuadCount, expectedGraphIncrease, expectedLabelStoreIncrease);
    }

    /**
     * Null testing for coverage's sake
     */
    @Test
    public void test_ingestData_nullLang() {
        // given
        DatasetGraphABAC datasetGraphABAC = mock(DatasetGraphABAC.class);
        // when
        // then
        assertThrows(ActionErrorException.class, () -> ingestData(getHttpAction(), "base", datasetGraphABAC, List.of()));
    }

    /**
     * Null testing for coverage's sake
     */
    @Test
    public void test_ingestData_triples_inputStreamNull() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("text/turtle");
        when(MOCK_REQUEST.getInputStream()).thenReturn(null);
        // when
        // then
        assertThrows(RiotException.class, () -> ingestData(getHttpAction(), "base", datasetGraphABAC, List.of()));
    }

    /**
     * Null testing for coverage's sake
     */
    @Test
    public void test_ingestData_quads_inputStreamNull() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("application/n-quads");
        when(MOCK_REQUEST.getInputStream()).thenReturn(null);
        // when
        // then
        assertThrows(RiotException.class, () -> ingestData(getHttpAction(), "base", datasetGraphABAC, List.of()));
    }


    private HttpAction getHttpAction() {
        return new HttpAction(1L, LOGGER, ACTION, MOCK_REQUEST, MOCK_RESPONSE);
    }

    private static class TestServletInputStream extends ServletInputStream {

        private final InputStream inputStream;

        public TestServletInputStream(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public int read() throws IOException {
            return this.inputStream.read();
        }

        @Override
        public void close() throws IOException {
            super.close();
            this.inputStream.close();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
        }
    }

}
