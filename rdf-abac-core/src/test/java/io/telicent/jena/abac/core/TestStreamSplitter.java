package io.telicent.jena.abac.core;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TestStreamSplitter {

    @Test
    public void test_prefix() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<String> stringList = List.of();
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        cut.prefix("prefix", "http://example/prefix/");
        graph.add(Triple.create(NodeFactory.createBlankNode(), NodeFactory.createURI("prefix:test1"), NodeFactory.createURI("prefix:test2")));
        // then
        assertFalse(graph.getPrefixMapping().hasNoMappings());
        assertEquals("http://example/prefix/", graph.getPrefixMapping().getNsPrefixURI("prefix"));
        assertEquals("prefix", graph.getPrefixMapping().getNsURIPrefix("http://example/prefix/"));
    }

    @Test
    public void test_triple_labelsNull() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<String> stringList = null;
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Triple triple = Triple.create(NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.triple(triple);
        // then
        assertTrue(graph.isEmpty());
    }

    @Test
    public void test_triple_labelsEmpty() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<String> stringList = List.of();
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Triple triple = Triple.create(NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.triple(triple);
        // then
        assertTrue(graph.isEmpty());
    }

    @Test
    public void test_triple_labelsOneEntry() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<String> stringList = List.of("LABEL");
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Triple triple = Triple.create(NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.triple(triple);
        // then
        assertFalse(graph.isEmpty());
        assertEquals(2, graph.size());
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        Stream<Triple> tripleStream = graph.stream(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        Triple match = tripleStream.findFirst().get();
        assertNotNull(match);
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL")));
    }

    @Test
    public void test_triple_labelsMultipleEntry() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<String> stringList = List.of("LABEL-1","LABEL-2", "LABEL-3");
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Triple triple = Triple.create(NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.triple(triple);
        // then
        assertFalse(graph.isEmpty());
        assertEquals(4, graph.size());
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        Stream<Triple> tripleStream = graph.stream(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        Triple match = tripleStream.findFirst().get();
        assertNotNull(match);
        assertTrue(graph.contains(match.getSubject(), VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL-1")));
        assertTrue(graph.contains(match.getSubject(), VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL-2")));
        assertTrue(graph.contains(match.getSubject(), VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL-3")));
    }

    @Test
    public void test_quad_defaultGraphs_labelsNull() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<String> stringList = null;
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Triple triple = Triple.create(NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.triple(triple);
        // then
        assertTrue(graph.isEmpty());
    }

    @Test
    public void test_quad_defaultGraph_labelsEmpty() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<String> stringList = List.of();
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.quad(quad);
        // then
        assertTrue(graph.isEmpty());
    }

    @Test
    public void test_quad_defaultGraph_labelsOneEntry() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<String> stringList = List.of("LABEL");
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.quad(quad);
        // then
        assertFalse(graph.isEmpty());
        assertEquals(2, graph.size());
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        Stream<Triple> tripleStream = graph.stream(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        Triple match = tripleStream.findFirst().get();
        assertNotNull(match);
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL")));
    }

    @Test
    public void test_quad_defaultGraph_labelsMultipleEntry() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<String> stringList = List.of("LABEL-1","LABEL-2", "LABEL-3");
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.quad(quad);
        // then
        assertFalse(graph.isEmpty());
        assertEquals(4, graph.size());
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        Stream<Triple> tripleStream = graph.stream(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        Triple match = tripleStream.findFirst().get();
        assertNotNull(match);
        assertTrue(graph.contains(match.getSubject(), VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL-1")));
        assertTrue(graph.contains(match.getSubject(), VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL-2")));
        assertTrue(graph.contains(match.getSubject(), VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL-3")));
    }

    public class TestStreamRDF implements StreamRDF {

        @Override
        public void start() {

        }

        @Override
        public void triple(Triple triple) {

        }

        @Override
        public void quad(Quad quad) {

        }

        @Override
        public void base(String base) {

        }

        @Override
        public void prefix(String prefix, String iri) {

        }

        @Override
        public void finish() {

        }
    }
}
