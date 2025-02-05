package io.telicent.jena.abac.labels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestL {
    @Test
    public void test_parsePattern_tooManyTokens() {
        // given
        // when
        // then
        assertThrows(AuthzTriplePatternException.class, () -> L.parsePattern("'subject' 'predicate' 'xyz' 'extra'", null));
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
        TriplePattern pattern = L.parsePattern("_:146c6e15-f44a-4a1d-bfcb-3d830f9b5af3 'predicate' 'object'", null);
        // then
        assertNotNull(pattern);
    }

    @Test
    public void test_parsePattern_underScore() {
        // given
        // when
        TriplePattern pattern = L.parsePattern("_ 'predicate' 'object'", null);
        // then
        assertNotNull(pattern);
    }
}