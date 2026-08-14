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

package io.telicent.jena.abac.core;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TestCoreCoverageHelpers {

    @Test
    void initABAC_stopAndLevel() {
        InitABAC init = new InitABAC();
        init.stop();
        assertEquals(200, init.level());
    }

    @Test
    void writeWithLabels_writesTriplesAndLabelComments() throws Exception {
        try (LabelsStore labelStore = Labels.createLabelsStoreMem()) {
            var base = DatasetGraphFactory.createTxnMem();
            Triple triple = Triple.create(NodeFactory.createURI("http://example/s"),
                                          NodeFactory.createURI("http://example/p"),
                                          NodeFactory.createLiteralString("o"));
            base.add(Quad.create(Quad.defaultGraphIRI, triple));
            labelStore.add(Quad.create(Quad.defaultGraphIRI, triple), Label.fromText("alpha"));

            DatasetGraphABAC dsg = new DatasetGraphABAC(base, null, labelStore, null, mock(AttributesStore.class));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            LabelledDataWriter.writeWithLabels(out, dsg);

            String serialized = out.toString(StandardCharsets.UTF_8);
            assertTrue(serialized.contains("http://example/s"));
            assertTrue(serialized.contains("//alpha"));
        }
    }
}
