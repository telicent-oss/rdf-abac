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

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Quad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLCoverageHelpers {

    @Test
    void newLabelGraph_setsExpectedPrefixes() {
        Graph graph = L.newLabelGraph();
        assertNotNull(graph);
        assertNotNull(graph.getPrefixMapping().getNsPrefixURI("authz"));
    }

    @Test
    void quadToString_handlesDefaultAndNamedGraphs() {
        Quad defaultGraphQuad = Quad.create(Quad.defaultGraphIRI,
                                            NodeFactory.createURI("http://example/s"),
                                            NodeFactory.createURI("http://example/p"),
                                            NodeFactory.createLiteralString("o"));
        Quad namedGraphQuad = Quad.create(NodeFactory.createURI("http://example/g"),
                                          NodeFactory.createURI("http://example/s"),
                                          NodeFactory.createURI("http://example/p"),
                                          NodeFactory.createLiteralString("o"));

        String tripleLike = L.quadToString(defaultGraphQuad);
        String quadLike = L.quadToString(namedGraphQuad);

        assertTrue(tripleLike.contains("http://example/s"));
        assertTrue(quadLike.contains("http://example/g"));
        assertEquals(tripleLike, L.quadToString(defaultGraphQuad));
    }
}
