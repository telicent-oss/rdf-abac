package io.telicent.jena.abac.core;

import io.telicent.jena.abac.labels.Label;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static io.telicent.jena.abac.core.VocabAuthz.graphForLabels;
import static org.junit.jupiter.api.Assertions.*;

public class TestStreamSplitter {

    @Test
    public void test_prefix() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<Label> stringList = List.of();
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
        List<Label> stringList = null;
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
        List<Label> stringList = List.of();
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
        List<Label> stringList = List.of(Label.fromText("LABEL"));
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Triple triple = Triple.create(NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.triple(triple);
        // then
        assertFalse(graph.isEmpty());
        assertEquals(2, graph.size());
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        Stream<Triple> tripleStream = graph.stream(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        Optional<Triple> optionalMatch = tripleStream.findFirst();
        assertTrue(optionalMatch.isPresent());
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL")));
    }

    @Test
    public void test_triple_labelsMultipleEntry() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<Label> stringList = List.of(Label.fromText("LABEL-1"), Label.fromText("LABEL-2"), Label.fromText("LABEL-3"));
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Triple triple = Triple.create(NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.triple(triple);
        // then
        assertFalse(graph.isEmpty());
        assertEquals(4, graph.size());
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        Stream<Triple> tripleStream = graph.stream(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        Optional<Triple> optionalMatch = tripleStream.findFirst();
        assertTrue(optionalMatch.isPresent());
        Triple match = optionalMatch.get();
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
        List<Label> stringList = null;
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
        List<Label> stringList = List.of();
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
        List<Label> stringList = List.of(Label.fromText("LABEL"));
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.quad(quad);
        // then
        assertFalse(graph.isEmpty());
        assertEquals(2, graph.size());
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        Stream<Triple> tripleStream = graph.stream(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        Optional<Triple> optionalMatch = tripleStream.findFirst();
        assertTrue(optionalMatch.isPresent());
        Triple match = optionalMatch.get();
        assertNotNull(match);
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL")));
    }

    @Test
    public void test_quad_defaultGraph_labelsMultipleEntry() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<Label> stringList = List.of(Label.fromText("LABEL-1"), Label.fromText("LABEL-2"), Label.fromText("LABEL-3"));
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(Quad.defaultGraphIRI, NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test"), NodeFactory.createBlankNode());
        cut.quad(quad);
        // then
        assertFalse(graph.isEmpty());
        assertEquals(4, graph.size());
        // Use ANY since we don't know the Blank Node Ids
        assertTrue(graph.contains(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY));
        Stream<Triple> tripleStream = graph.stream(Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        Optional<Triple> optionalMatch = tripleStream.findFirst();
        assertTrue(optionalMatch.isPresent());
        Triple match = optionalMatch.get();
        assertNotNull(match);
        assertTrue(graph.contains(match.getSubject(), VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL-1")));
        assertTrue(graph.contains(match.getSubject(), VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL-2")));
        assertTrue(graph.contains(match.getSubject(), VocabAuthzLabels.pLabel, NodeFactory.createLiteralString("LABEL-3")));
    }

    @Test
    public void test_quad_namedGraphForLabels() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<Label> stringList = List.of(Label.fromText("LABEL-1"), Label.fromText("LABEL-2"), Label.fromText("LABEL-3"));
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(graphForLabels, NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test-label"), NodeFactory.createBlankNode());
        cut.quad(quad);
        // then
        assertFalse(graph.isEmpty());
        assertEquals(1, graph.size());
        // Use ANY since we don't know the Blank Node Ids
        assertTrue(graph.contains(Node.ANY, NodeFactory.createLiteralString("test-label"), Node.ANY));
    }

    @Test
    public void test_quad_otherNamedGraph_doesNotUpdateLabels() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<Label> stringList = List.of(Label.fromText("LABEL-1"), Label.fromText("LABEL-2"), Label.fromText("LABEL-3"));
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(NodeFactory.createURI("http://example/unrecognisedNamedGraph"), NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test-predicate"), NodeFactory.createBlankNode());
        cut.quad(quad);
        // then
        assertTrue(graph.isEmpty());
    }

    @Test
    public void test_quad_literalNamedGraph_doesNotUpdateLabels() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<Label> stringList = List.of(Label.fromText("LABEL-1"), Label.fromText("LABEL-2"), Label.fromText("LABEL-3"));
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(NodeFactory.createLiteralString("named-graph"), NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test-predicate"), NodeFactory.createBlankNode());
        cut.quad(quad);
        // then
        assertTrue(graph.isEmpty());
    }

    @Test
    public void test_quad_namedGraph_withReservedName() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<Label> stringList = List.of(Label.fromText("LABEL-1"), Label.fromText("LABEL-2"), Label.fromText("LABEL-3"));
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(NodeFactory.createURI("http://telicent.io/security#/incorrect"), NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test-predicate"), NodeFactory.createBlankNode());
        cut.quad(quad);
        // then
        assertTrue(graph.isEmpty());
        Set<String> warnings = cut.getWarningsIssued();
        assertFalse(warnings.isEmpty());
        assertEquals(1, warnings.size());
    }

    @Test
    public void test_quad_namedGraph_withReservedName_onlyRecordedOnce() {
        // given
        StreamRDF data = new TestStreamRDF();
        Graph graph = GraphFactory.createDefaultGraph();
        List<Label> stringList = List.of(Label.fromText("LABEL-1"),Label.fromText("LABEL-2"),Label.fromText("LABEL-3"));
        StreamSplitter cut = new StreamSplitter(data, graph, stringList);
        // when
        Quad quad = Quad.create(NodeFactory.createURI("http://telicent.io/security#/incorrect"), NodeFactory.createBlankNode(), NodeFactory.createLiteralString("test-predicate"), NodeFactory.createBlankNode());
        cut.quad(quad);
        cut.quad(quad);
        // then
        assertTrue(graph.isEmpty());
        Set<String> warnings = cut.getWarningsIssued();
        assertFalse(warnings.isEmpty());
        assertEquals(1, warnings.size());
    }


    public static class TestStreamRDF implements StreamRDF {

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
