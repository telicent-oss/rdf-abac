/**
 * Copyright (C) Telicent Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.telicent.smart.cache.security.data.plugins.rdf.abac;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.attributes.syntax.AEX;
import io.telicent.jena.abac.core.AttributesStoreLocal;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.security.data.DataSecurityException;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.*;

public class TestRdfAbacLabelsRemover {

    private static final Quad TEST_QUAD = Quad.create(
            Quad.defaultGraphIRI,
            NodeFactory.createURI("http://s"),
            NodeFactory.createURI("http://p"),
            NodeFactory.createURI("http://o"));

    // RdfAbacLabelRemover is deprecated but tests should remain until the class itself is deleted
    private RdfAbacLabelsRemover remover; //NOSONAR

    @BeforeMethod
    public void setUp() {
        remover = new RdfAbacLabelsRemover(); //NOSONAR
    }

    @Test
    public void givenNonAbacDataset_whenRemoving_thenNoOpWithoutError() throws DataSecurityException {
        final DatasetGraph plainDsg = DatasetGraphFactory.createTxnMem();
        remover.remove(plainDsg, TEST_QUAD);
        // No exception — remove is silently skipped for non-ABAC datasets
    }

    @Test
    public void givenAbacDataset_whenRemoving_thenLabelIsRemoved() throws DataSecurityException {
        // Use a real labels store to verify remove actually clears the label
        final LabelsStore labelsStore = Labels.createLabelsStoreMem();
        final DatasetGraphABAC abac = ABAC.authzDataset(DatasetGraphFactory.createTxnMem(),
                AEX.strALLOW, labelsStore, SysABAC.denyLabel, new AttributesStoreLocal());

        // Pre-condition: add a label for the quad
        labelsStore.add(TEST_QUAD.getGraph(), TEST_QUAD.getSubject(), TEST_QUAD.getPredicate(),
                TEST_QUAD.getObject(), Label.fromText("clearance=S"));
        Assert.assertNotNull(labelsStore.labelForQuad(TEST_QUAD));

        remover.remove(abac, TEST_QUAD);

        Assert.assertNull(labelsStore.labelForQuad(TEST_QUAD));
    }

    @Test
    public void givenAbacDataset_whenRemoving_thenLiveStoreIsNotClosed() throws Exception {
        final LabelsStore labelsStore = mock(LabelsStore.class);
        final DatasetGraphABAC abac = ABAC.authzDataset(DatasetGraphFactory.createTxnMem(),
                AEX.strALLOW, labelsStore, SysABAC.denyLabel, new AttributesStoreLocal());

        // Pre-condition: add a label for the quad
        labelsStore.add(TEST_QUAD.getGraph(), TEST_QUAD.getSubject(), TEST_QUAD.getPredicate(),
                TEST_QUAD.getObject(), Label.fromText("clearance=S"));

        remover.remove(abac, TEST_QUAD);

        verify(labelsStore, never()).close();
    }

    @Test(expectedExceptions = DataSecurityException.class)
    public void givenLabelsStoreThatThrows_whenRemoving_thenDataSecurityException() throws DataSecurityException {
        final LabelsStore labelsStore = mock(LabelsStore.class);
        // doThrow without specifying overload — Mockito can handle this via proxy
        doAnswer(invocation -> { throw new RuntimeException("remove failed"); }).when(labelsStore).remove(TEST_QUAD);
        final DatasetGraphABAC abac = ABAC.authzDataset(DatasetGraphFactory.createTxnMem(),
                AEX.strALLOW, labelsStore, SysABAC.denyLabel, new AttributesStoreLocal());

        remover.remove(abac, TEST_QUAD);
    }



}
