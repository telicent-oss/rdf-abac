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
import io.telicent.smart.cache.security.data.DataSecurityException;
import io.telicent.smart.cache.security.data.labels.DatasetGraphLabelled;
import io.telicent.smart.cache.security.data.labels.MalformedLabelsException;
import io.telicent.smart.cache.security.data.labels.SecurityLabels;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.system.Txn;
import io.telicent.smart.cache.storage.BackupRestoreCapable;
import io.telicent.smart.cache.storage.CompactCapable;
import io.telicent.smart.cache.storage.labels.LabelsStore;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

public class TestRdfAbacDatasetGraphLabelled {

    private static final Quad QUAD = Quad.create(NodeFactory.createURI("http://example.org/g"),
                                                 NodeFactory.createURI("http://example.org/s"),
                                                 NodeFactory.createURI("http://example.org/p"),
                                                 NodeFactory.createLiteralString("o"));

    private DatasetGraphABAC abac;
    private DatasetGraphLabelled labelled;

    @BeforeMethod
    public void setup() {
        this.abac = ABAC.authzDataset(DatasetGraphFactory.createTxnMem(), AEX.strALLOW, Labels.createLabelsStoreMem(),
                                      SysABAC.denyLabel, new AttributesStoreLocal());
        this.labelled = new RdfAbacPlugin().prepareLabelledDataset(this.abac).orElseThrow();
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullDataset_whenCreating_thenNPE() {
        new RdfAbacDatasetGraphLabelled(null, new RdfAbacParser());
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullParser_whenCreating_thenNPE() {
        new RdfAbacDatasetGraphLabelled(this.abac, null);
    }

    @Test
    public void givenLabelledDataset_whenInspecting_thenRdfAbacDetails() {
        Assert.assertEquals(this.labelled.labelsGraphName(), VocabAuthz.graphForLabels);
        Assert.assertNotNull(this.labelled.labelsParser());
    }

    @Test
    public void givenLabelledDataset_whenAddingLabels_thenStoredInSameTransactionAsData() {
        // Given
        final SecurityLabels<?> labels = this.labelled.labelsParser().parseSecurityLabels("clearance=O".getBytes(StandardCharsets.UTF_8));

        // When
        Txn.executeWrite(this.labelled, () -> {
            this.labelled.add(QUAD);
            this.labelled.addLabels(List.of(QUAD), labels);
        });

        // Then
        Assert.assertTrue(Txn.calculateRead(this.abac, () -> this.abac.contains(QUAD)));
        Assert.assertEquals(Txn.calculateRead(this.abac, () -> this.abac.labelsStore().labelForQuad(QUAD)),
                            Label.fromText("clearance=O"));
    }

    @Test
    public void givenLabelledDataset_whenAddingLabelsGraph_thenFineGrainedLabelsStored() {
        // Given
        final Graph graph = RDFParser.fromString("""
                                                   PREFIX authz: <http://telicent.io/security#>
                                                   [] authz:pattern '<http://example.org/s> <http://example.org/p> "o"' ;
                                                      authz:label 'clearance=S' .
                                                   """, Lang.TTL).toGraph();

        // When
        Txn.executeWrite(this.labelled, () -> this.labelled.addLabelsGraph(graph));

        // Then
        final Quad expected = Quad.create(Quad.defaultGraphIRI, QUAD.getSubject(), QUAD.getPredicate(), QUAD.getObject());
        Assert.assertEquals(Txn.calculateRead(this.abac, () -> this.abac.labelsStore().labelForQuad(expected)),
                            Label.fromText("clearance=S"));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullQuads_whenAddingLabels_thenNPE() {
        this.labelled.addLabels(null, this.labelled.labelsParser().parseSecurityLabels("a=b".getBytes(StandardCharsets.UTF_8)));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullLabels_whenAddingLabels_thenNPE() {
        this.labelled.addLabels(List.of(QUAD), null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullGraph_whenAddingLabelsGraph_thenNPE() {
        this.labelled.addLabelsGraph(null);
    }

    @Test(expectedExceptions = MalformedLabelsException.class)
    public void givenInvalidLabelText_whenParsingViaLabelledDataset_thenMalformed() {
        this.labelled.labelsParser().parseSecurityLabels("(((".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    public void givenInMemoryLabelsStore_whenGettingLabelsStore_thenEmpty() {
        Assert.assertTrue(this.labelled.labelsStore().isEmpty());
    }

    @Test
    public void givenLabelsStoreThatIsAlsoStorageLabelsStore_whenGettingLabelsStore_thenSameStoreAndNotInteractedWith() {
        // Given
        final var store = mock(io.telicent.jena.abac.labels.LabelsStore.class,
                     withSettings().extraInterfaces(LabelsStore.class, BackupRestoreCapable.class,
                                                    CompactCapable.class));
        final DatasetGraphABAC dataset = mock(DatasetGraphABAC.class);
        when(dataset.labelsStore()).thenReturn(store);
        final DatasetGraphLabelled labelledGraph = new RdfAbacDatasetGraphLabelled(dataset, new RdfAbacParser());

        // When
        final LabelsStore actual = labelledGraph.labelsStore().orElseThrow();

        // Then
        Assert.assertSame(actual, store);
        Assert.assertTrue(actual instanceof BackupRestoreCapable);
        Assert.assertTrue(actual instanceof CompactCapable);
        verifyNoInteractions(store);
    }

    @Test
    public void givenLabelledQuad_whenRemovingLabels_thenLabelCleared() {
        // Given
        final SecurityLabels<?> labels = this.labelled.labelsParser().parseSecurityLabels("clearance=O".getBytes(StandardCharsets.UTF_8));
        Txn.executeWrite(this.labelled, () -> {
            this.labelled.add(QUAD);
            this.labelled.addLabels(List.of(QUAD), labels);
        });
        Assert.assertNotNull(Txn.calculateRead(this.abac, () -> this.abac.labelsStore().labelForQuad(QUAD)));

        // When
        Txn.executeWrite(this.labelled, () -> {
            try {
                this.labelled.removeLabels(QUAD);
            } catch (DataSecurityException e) {
                throw new IllegalStateException(e);
            }
        });

        // Then
        Assert.assertNull(Txn.calculateRead(this.abac, () -> this.abac.labelsStore().labelForQuad(QUAD)));
    }

    @Test
    public void givenLabelsStore_whenRemovingLabels_thenStoreNotClosed() throws Exception {
        final var store = mock(io.telicent.jena.abac.labels.LabelsStore.class);
        final var dataset = mock(DatasetGraphABAC.class);
        when(dataset.labelsStore()).thenReturn(store);

        new RdfAbacDatasetGraphLabelled(dataset, new RdfAbacParser()).removeLabels(QUAD);

        verify(store).remove(QUAD);
        verify(store, never()).close();
    }

    @Test(expectedExceptions = DataSecurityException.class, expectedExceptionsMessageRegExp = "store failed")
    public void givenFailingLabelsStore_whenRemovingLabels_thenDataSecurityException() throws Exception {
        final var store = mock(io.telicent.jena.abac.labels.LabelsStore.class);
        doThrow(new IllegalStateException("store failed")).when(store).remove(QUAD);
        final var dataset = mock(DatasetGraphABAC.class);
        when(dataset.labelsStore()).thenReturn(store);

       new RdfAbacDatasetGraphLabelled(dataset, new RdfAbacParser()).removeLabels(QUAD);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullQuad_whenRemovingLabels_thenNPE() throws Exception {
        this.labelled.removeLabels(null);
    }

    @Test
    public void givenLabelsStoreWithMetrics_whenGettingMetrics_thenStoreMetricsReturned() {
        final io.telicent.jena.abac.labels.LabelsStore store = mock(io.telicent.jena.abac.labels.LabelsStore.class);
        when(store.getMetrics()).thenReturn(Map.of(DatasetGraphLabelled.METRIC_LABEL_WRITES, 3L));
        final var dataset = mock(DatasetGraphABAC.class);
        when(dataset.labelsStore()).thenReturn(store);

        Assert.assertEquals(new RdfAbacDatasetGraphLabelled(dataset, new RdfAbacParser()).labelsMetrics(),
                            Map.of(DatasetGraphLabelled.METRIC_LABEL_WRITES, 3L));
    }

    @Test
    public void givenInMemoryLabelsStore_whenGettingMetrics_thenNoMetrics() {
        Assert.assertTrue(this.labelled.labelsMetrics().isEmpty());
    }

    @Test
    public void givenMetricConstants_whenComparedToRdfAbac_thenIdentical() {
        Assert.assertEquals(DatasetGraphLabelled.METRIC_LABEL_ADD_ATTEMPTS,
                            io.telicent.jena.abac.labels.LabelsStore.METRIC_LABEL_ADD_ATTEMPTS);
        Assert.assertEquals(DatasetGraphLabelled.METRIC_LABEL_CACHE_NO_OPS,
                            io.telicent.jena.abac.labels.LabelsStore.METRIC_LABEL_CACHE_NO_OPS);
        Assert.assertEquals(DatasetGraphLabelled.METRIC_LABEL_WRITES,
                            io.telicent.jena.abac.labels.LabelsStore.METRIC_LABEL_WRITES);
    }
}
