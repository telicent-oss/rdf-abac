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
import io.telicent.jena.abac.core.VocabAuthz;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.system.Txn;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class TestRdfChangesApplyWithLabels {

    private static final Node S = NodeFactory.createURI("http://s");
    private static final Node P = NodeFactory.createURI("http://p");
    private static final Node O = NodeFactory.createURI("http://o");
    private static final Node GRAPH = NodeFactory.createURI("http://graph");
    private static final Label LABEL = Label.fromText("clearance=S");

    private DatasetGraphABAC abac;

    @BeforeMethod
    public void setUp() {
        abac = ABAC.authzDataset(DatasetGraphFactory.createTxnMem(),
                AEX.strALLOW, Labels.createLabelsStoreMem(), SysABAC.denyLabel, new AttributesStoreLocal());
    }

    @Test
    public void givenLabelAndNoDistributionId_whenAdding_thenTripleStoredWithLabel() {
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL);
        changes.txnBegin();
        changes.add(GRAPH, S, P, O);
        changes.txnCommit();

        Assert.assertTrue(abac.contains(GRAPH, S, P, O));
        Label stored = abac.labelsStore().labelForQuad(Quad.create(GRAPH, S, P, O));
        Assert.assertEquals(stored, LABEL);
    }

    @Test
    public void givenNullLabel_whenAdding_thenTripleStoredWithoutLabel() {
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, null);
        changes.txnBegin();
        changes.add(GRAPH, S, P, O);
        changes.txnCommit();

        Assert.assertTrue(abac.contains(GRAPH, S, P, O));
        Label stored = abac.labelsStore().labelForQuad(Quad.create(GRAPH, S, P, O));
        Assert.assertNull(stored);
    }

    @Test
    public void givenDistributionId_whenAdding_thenTripleStoredInDistributionGraph() {
        final String distributionId = "http://distribution/1";
        final Node distGraph = NodeFactory.createURI(distributionId);

        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL, distributionId);
        changes.txnBegin();
        changes.add(GRAPH, S, P, O);
        changes.txnCommit();

        Assert.assertTrue(abac.contains(distGraph, S, P, O));
        Assert.assertFalse(abac.contains(GRAPH, S, P, O));
    }

    @Test
    public void givenLabelsGraphQuad_whenAdding_thenNotStoredAsDataTriple() {
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL);
        changes.txnBegin();
        changes.add(VocabAuthz.graphForLabels, S, P, O);
        changes.txnCommit();

        Assert.assertFalse(abac.contains(VocabAuthz.graphForLabels, S, P, O));
    }

    @Test
    public void givenAddedTriple_whenDeleting_thenTripleRemoved() {
        // First add via commit
        final RdfAbacChangesApplyWithLabels add = new RdfAbacChangesApplyWithLabels(abac, LABEL);
        add.txnBegin();
        add.add(GRAPH, S, P, O);
        add.txnCommit();
        Assert.assertTrue(abac.contains(GRAPH, S, P, O));

        // Then delete in a new transaction
        final RdfAbacChangesApplyWithLabels delete = new RdfAbacChangesApplyWithLabels(abac, LABEL);
        delete.txnBegin();
        delete.delete(GRAPH, S, P, O);
        delete.txnCommit();
        Assert.assertFalse(abac.contains(GRAPH, S, P, O));
    }

    @Test
    public void givenLabelsGraphQuad_whenDeleting_thenNotTreatedAsDataDelete() {
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL);
        changes.txnBegin();
        changes.delete(VocabAuthz.graphForLabels, S, P, O);
        changes.txnCommit();
        // No exception — labels-graph deletes are tracked internally, not as data
    }

    @Test
    public void givenTransactionBeginCommit_whenCommitting_thenChangesVisible() {
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL);
        changes.txnBegin();
        changes.add(GRAPH, S, P, O);
        changes.txnCommit();
        Assert.assertTrue(abac.contains(GRAPH, S, P, O));
    }

    @Test
    public void givenTransactionBeginAbort_whenAborting_thenChangesNotVisible() {
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL);
        changes.txnBegin();
        changes.add(GRAPH, S, P, O);
        changes.txnAbort();
        Assert.assertFalse(abac.contains(GRAPH, S, P, O));
    }

    @Test
    public void givenNullGraph_whenAdding_thenGraphSetToDefault() {
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL);
        changes.txnBegin();
        changes.add(null, S, P, O);
        changes.txnCommit();
        changes.finish();
        Assert.assertTrue(abac.contains(Quad.defaultGraphIRI, S, P, O));
    }

    @Test
    public void givenNullGraph_whenAddingAndDeleting_thenGraphSetToDefault() {
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL);
        changes.txnBegin();
        changes.add(null, S, P, O);
        changes.delete(null, S, P, O);
        changes.txnCommit();
        changes.finish();
        Assert.assertFalse(abac.contains(Quad.defaultGraphIRI, S, P, O));
    }

    @Test
    public void givenNullGraph_whenAddingAndDeleting_thenTargetGraphAffected() {
        final String distributionId = "http://distribution/1";
        final Node distGraph = NodeFactory.createURI(distributionId);
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL, distributionId);

        changes.txnBegin();
        changes.add(null, S, P, O);
        changes.txnCommit();
        Assert.assertTrue(abac.contains(distGraph, S, P, O));
        changes.txnBegin();
        changes.delete(null, S, P, O);
        changes.txnCommit();
        changes.finish();
        Assert.assertFalse(abac.contains(distGraph, S, P, O));
    }

    @Test
    public void givenDuplicateQuads_whenCommitting_thenLabelsAreAppliedAsOneBatch() {
        LabelsStore labelsStore = spy(Labels.createLabelsStoreMem());
        DatasetGraphABAC dataset = ABAC.authzDataset(DatasetGraphFactory.createTxnMem(),
                AEX.strALLOW, labelsStore, SysABAC.denyLabel, new AttributesStoreLocal());
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(dataset, LABEL);

        changes.txnBegin();
        changes.add(GRAPH, S, P, O);
        changes.add(GRAPH, S, P, O);
        changes.txnCommit();

        verify(labelsStore, times(1)).addAll(any(), org.mockito.ArgumentMatchers.eq(LABEL));
        Assert.assertEquals(labelsStore.labelForQuad(Quad.create(GRAPH, S, P, O)), LABEL);
    }

    @Test
    public void givenAbortedBatch_whenFinishing_thenLabelsAreNotApplied() {
        LabelsStore labelsStore = spy(Labels.createLabelsStoreMem());
        DatasetGraphABAC dataset = ABAC.authzDataset(DatasetGraphFactory.createTxnMem(),
                AEX.strALLOW, labelsStore, SysABAC.denyLabel, new AttributesStoreLocal());
        final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(dataset, LABEL);

        changes.txnBegin();
        changes.add(GRAPH, S, P, O);
        changes.txnAbort();
        changes.finish();

        verify(labelsStore, times(0)).addAll(any(), org.mockito.ArgumentMatchers.eq(LABEL));
        Assert.assertNull(labelsStore.labelForQuad(Quad.create(GRAPH, S, P, O)));
    }

    @Test
    public void givenExternalTransactionWithoutPatchTransaction_whenFinishing_thenLabelsAreApplied() {
        Txn.executeWrite(abac, () -> {
            final RdfAbacChangesApplyWithLabels changes = new RdfAbacChangesApplyWithLabels(abac, LABEL);
            changes.add(GRAPH, S, P, O);
            changes.finish();
        });

        Assert.assertEquals(abac.labelsStore().labelForQuad(Quad.create(GRAPH, S, P, O)), LABEL);
    }

}
