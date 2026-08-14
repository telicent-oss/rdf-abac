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

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TestLabelsStoreOne {

    @Test
    void testFixedLabelStoreBehaviour() throws Exception {
        Label label = Label.fromText("fixed");
        LabelsStoreOne store = new LabelsStoreOne(label);
        Quad concrete = Quad.create(Quad.defaultGraphIRI,
                                    NodeFactory.createURI("http://example/s"),
                                    NodeFactory.createURI("http://example/p"),
                                    NodeFactory.createLiteralString("o"));

        assertNotNull(store.getTransactional());
        assertEquals(label, store.labelForQuad(concrete));
        assertNull(store.labelForQuad(Quad.ANY));
        assertThrows(UnsupportedOperationException.class, () -> store.add(concrete, label));
        assertThrows(UnsupportedOperationException.class, () -> store.addGraph(GraphFactory.createDefaultGraph()));
        assertThrows(UnsupportedOperationException.class, () -> store.remove(concrete));
        assertTrue(store.isEmpty());
        assertNull(store.asGraph());
        assertEquals(Map.of(), store.getProperties());

        AtomicReference<Quad> seenQuad = new AtomicReference<>();
        AtomicReference<Label> seenLabel = new AtomicReference<>();
        store.forEach((quad, itemLabel) -> {
            seenQuad.set(quad);
            seenLabel.set(itemLabel);
        });
        assertEquals(Quad.ANY, seenQuad.get());
        assertEquals(label, seenLabel.get());

        assertDoesNotThrow(store::close);
    }
}
