/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.core.AuthzException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestABACPattern {

    private static final Node S = NodeFactory.createURI("http://example/s");
    private static final Node P = NodeFactory.createURI("http://example/p");
    private static final Node O = NodeFactory.createLiteralString("o");

    @Test
    void testFromTripleCoversSupportedPatterns() {
        assertEquals(ABACPattern.PatternSPO, ABACPattern.fromTriple(Triple.create(S, P, O)));
        assertEquals(ABACPattern.PatternSP_, ABACPattern.fromTriple(S, P, Node.ANY));
        assertEquals(ABACPattern.PatternS__, ABACPattern.fromTriple(S, Node.ANY, Node.ANY));
        assertEquals(ABACPattern.Pattern_P_, ABACPattern.fromTriple(Node.ANY, P, Node.ANY));
        assertEquals(ABACPattern.Pattern___, ABACPattern.fromTriple(Node.ANY, Node.ANY, Node.ANY));
    }

    @Test
    void testFromTripleRejectsUnsupportedPattern() {
        assertThrows(AuthzException.class, () -> ABACPattern.fromTriple(Node.ANY, P, O));
    }
}
