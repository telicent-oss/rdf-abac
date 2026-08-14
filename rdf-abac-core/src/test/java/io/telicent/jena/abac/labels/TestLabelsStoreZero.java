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
import org.apache.jena.sparql.graph.GraphZero;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

class TestLabelsStoreZero {

    @Test
    void testEmptyLabelStoreBehaviour() throws Exception {
        LabelsStoreZero store = new LabelsStoreZero();
        Quad concrete = Quad.create(Quad.defaultGraphIRI,
                                    NodeFactory.createURI("http://example/s"),
                                    NodeFactory.createURI("http://example/p"),
                                    NodeFactory.createLiteralString("o"));

        assertNotNull(store.getTransactional());
        assertNull(store.labelForQuad(concrete));
        assertNull(store.labelForQuad(Quad.ANY));
        assertThrows(UnsupportedOperationException.class, () -> store.add(concrete, Label.fromText("x")));
        assertThrows(UnsupportedOperationException.class, () -> store.remove(concrete));
        assertTrue(store.isEmpty());
        assertTrue(store.asGraph() instanceof GraphZero);
        assertTrue(store.asGraph().isEmpty());
        assertEquals(Map.of(), store.getProperties());

        AtomicBoolean invoked = new AtomicBoolean(false);
        store.forEach((quad, label) -> invoked.set(true));
        assertFalse(invoked.get());

        assertDoesNotThrow(store::close);
    }
}
