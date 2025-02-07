package io.telicent.jena.abac.fuseki;


import io.telicent.jena.abac.core.DatasetGraphABAC;
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
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
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

    private static final String TTL_DATA = """
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

    private static final String NQ_DATA = """
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

    @Test
    public void test_ingestData_nullLang() {
        // given
        DatasetGraphABAC datasetGraphABAC = mock(DatasetGraphABAC.class);
        // when
        // then
        assertThrows(ActionErrorException.class, () -> ingestData(getHttpAction(), "base", datasetGraphABAC, List.of()));
    }

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

    @Test
    public void test_ingestData_triples_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("text/turtle");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(TTL_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of());
        // then
        assertNotNull(results);
        assertEquals(2, results.tripleCount());
        assertEquals(0, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(6, datasetGraphABAC.labelsStore().asGraph().size());
    }

    @Test
    public void test_ingestData_triples_nullLabels_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("text/turtle");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(TTL_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, null);
        // then
        assertNotNull(results);
        assertEquals(2, results.tripleCount());
        assertEquals(0, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
    }
    @Test
    public void test_ingestData_triples_defaultLabel_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("text/turtle");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(TTL_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of("default"));
        // then
        assertNotNull(results);
        assertEquals(2, results.tripleCount());
        assertEquals(0, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(8, datasetGraphABAC.labelsStore().asGraph().size());
    }

    @Test
    public void test_ingestData_triples_differentLabel_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("text/turtle");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(TTL_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of("different"));
        // then
        assertNotNull(results);
        assertEquals(2, results.tripleCount());
        assertEquals(0, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(8, datasetGraphABAC.labelsStore().asGraph().size());
    }

    @Test
    public void test_ingestData_triples_blankNodes_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("text/turtle");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(TTL_UNLABELLED_BLANK_NODE_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of("different"));
        // then
        assertNotNull(results);
        assertEquals(2, results.tripleCount());
        assertEquals(0, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(8, datasetGraphABAC.labelsStore().asGraph().size());
    }

    @Test
    public void test_ingestData_triples_labelledBlankNodes_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("text/turtle");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(TTL_LABELLED_BLANK_NODE_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of("different"));
        // then
        assertNotNull(results);
        assertEquals(2, results.tripleCount());
        assertEquals(0, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(8, datasetGraphABAC.labelsStore().asGraph().size());
    }


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

    @Test
    public void test_ingestData_quads_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("application/n-quads");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(NQ_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of());
        // then
        assertNotNull(results);
        assertEquals(0, results.tripleCount());
        assertEquals(2, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
    }

    @Test
    public void test_ingestData_quads_nullLabels_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("application/n-quads");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(NQ_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, null);
        // then
        assertNotNull(results);
        assertEquals(0, results.tripleCount());
        assertEquals(2, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
    }
    @Test
    public void test_ingestData_quads_defaultLabel_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("application/n-quads");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(NQ_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of("default"));
        // then
        assertNotNull(results);
        assertEquals(0, results.tripleCount());
        assertEquals(2, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(8, datasetGraphABAC.labelsStore().asGraph().size());
    }

    @Test
    public void test_ingestData_quads_differentLabel_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("application/n-quads");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(NQ_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of("different"));
        // then
        assertNotNull(results);
        assertEquals(0, results.tripleCount());
        assertEquals(2, results.quadCount());
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(8, datasetGraphABAC.labelsStore().asGraph().size());
    }

    @Test
    public void test_ingestData_quads_namedGraph_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("application/n-quads");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(NQ_NAMED_GRAPH_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of("givenlabels"));
        // then
        assertNotNull(results);
        assertEquals(0, results.tripleCount());
        assertEquals(2, results.quadCount()); // The Quads are added
        assertEquals(2, datasetGraphABAC.getDefaultGraph().size()); // makes no difference
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size()); // makes no difference
    }

    @Test
    public void test_ingestData_quads_labelledBlankNode_happyPath() throws IOException {
        // given
        FusekiServer server = server("server-labels/config-labels.ttl");
        DatasetGraph dsg = server.getDataAccessPointRegistry().get("/ds").getDataService().getDataset();
        DatasetGraphABAC datasetGraphABAC = (DatasetGraphABAC) dsg;

        when(MOCK_REQUEST.getContentType()).thenReturn("application/n-quads");
        TestServletInputStream inputStream = new TestServletInputStream(new ByteArrayInputStream(NQ_LABELLED_BLANK_NODE_DATA.getBytes()));
        when(MOCK_REQUEST.getInputStream()).thenReturn(inputStream);

        ServletOutputStream outputStream = mock(ServletOutputStream.class);
        when(MOCK_RESPONSE.getOutputStream()).thenReturn(outputStream);

        assertEquals(2, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(4, datasetGraphABAC.labelsStore().asGraph().size());
        // when
        LabelledDataLoader.UploadInfo results = ingestData(getHttpAction(), "base", datasetGraphABAC, List.of("givenlabel"));
        // then
        assertNotNull(results);
        assertEquals(0, results.tripleCount());
        assertEquals(2, results.quadCount()); // The Quads are added
        assertEquals(4, datasetGraphABAC.getDefaultGraph().size());
        assertEquals(8, datasetGraphABAC.labelsStore().asGraph().size());
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
