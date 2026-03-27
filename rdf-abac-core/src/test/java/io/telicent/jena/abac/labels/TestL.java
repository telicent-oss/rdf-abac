package io.telicent.jena.abac.labels;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Quad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestL {
    @Test
    public void test_parsePattern_tooManyTokens() {
        // given
        // when
        // then
        assertThrows(AuthzTriplePatternException.class, () -> L.parsePattern("'graph' 'subject' 'predicate' 'xyz' 'extra'", null));
    }

    @Test
    public void test_parsePattern_variableNode() {
        // given
        // when
        // then
        assertThrows(AuthzTriplePatternException.class, () -> L.parsePattern("?varable predicate object", null));
    }

    @Test
    public void test_parsePattern_blankNode() {
        // given
        // when
        Quad pattern = L.parsePattern("_:146c6e15-f44a-4a1d-bfcb-3d830f9b5af3 'predicate' 'object'", null);
        // then
        assertNotNull(pattern);
        assertEquals(Quad.defaultGraphIRI, pattern.getGraph());
    }

    @Test
    public void test_parsePattern_namedGraph() {
        // given
        // when
        Quad pattern = L.parsePattern("<https://example.org/graph> 'subject' 'predicate' 'object'", null);
        // then
        assertNotNull(pattern);
        assertEquals(NodeFactory.createURI("https://example.org/graph"), pattern.getGraph());
    }

    @Test
    public void test_parsePattern_underScore() {
        // given
        // when
        Quad pattern = L.parsePattern("_ 'predicate' 'object'", null);
        // then
        assertNotNull(pattern);
    }
}