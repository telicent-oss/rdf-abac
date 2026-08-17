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
import org.apache.jena.query.ReadWrite;
import org.apache.jena.sparql.core.Quad;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestLabelsStoreMemCoverage {

    @Test
    void standaloneReadFlushesAccumulatorIntoMainStore() throws Exception {
        try (LabelsStoreMem store = (LabelsStoreMem) LabelsStoreMem.create()) {
            Quad quad = concreteQuad();
            Label label = Label.fromText("alpha");

            store.add(quad, label);

            assertEquals("0", store.getProperties().get("size"));
            assertEquals(label, store.labelForQuad(quad));
            assertEquals("1", store.getProperties().get("size"));
            assertTrue(!store.isEmpty());
        }
    }

    @Test
    void wildcardAndAbortedWritesDoNotPopulateStore() throws Exception {
        try (LabelsStoreMem store = (LabelsStoreMem) LabelsStoreMem.create()) {
            Quad quad = concreteQuad();
            Label label = Label.fromText("alpha");

            assertNull(store.labelForQuad(Quad.ANY));
            store.add(Quad.ANY, label);
            assertTrue(store.isEmpty());

            store.getTransactional().begin(ReadWrite.WRITE);
            store.add(quad, label);
            store.getTransactional().abort();
            store.getTransactional().end();

            assertNull(store.labelForQuad(quad));
            assertTrue(store.isEmpty());
        }
    }

    private static Quad concreteQuad() {
        return Quad.create(Quad.defaultGraphIRI,
                           NodeFactory.createURI("http://example/s"),
                           NodeFactory.createURI("http://example/p"),
                           NodeFactory.createLiteralString("o"));
    }
}
