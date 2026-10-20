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
package io.telicent.smart.cache.security.data.plugins.rdf.abac.distribution;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.DatasetFilterProvider;
import io.telicent.jena.abac.DefaultDatasetFilterProvider;
import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.attributes.syntax.AEX;
import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.AttributesStoreLocal;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.security.data.distribution.DistributionLifecycleFilters;
import io.telicent.smart.cache.security.data.distribution.DistributionLifecycleStateFile;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import org.apache.jena.sparql.core.Quad;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

// java:S125 - retained test scaffolding
@SuppressWarnings("java:S125")
public class TestDistributionLifecycleFilters {

    private static final String STATE_FILE_PATH = "/tmp/scg-test-lifecycle-state.json";
    private static final String APPLICATION_ID = "some-application-id";
    private static final String ACTIVE_GRAPH = "http://example/active";
    private static final String WITHDRAWN_GRAPH = "http://example/withdrawn";

    private DatasetGraphABAC dataset;

    @BeforeMethod
    public void setUp() {
        this.dataset = ABAC.authzDataset(DatasetGraphFactory.createTxnMem(),
                                         AEX.strALLOW,
                                         Labels.createLabelsStoreMem(),
                                         SysABAC.denyLabel,
                                         new AttributesStoreLocal());
    }

    @Test
    public void installIfConfigured_returnsFalse_whenLifecycleFilterAlreadyInstalled() {
        final DistributionLifecycleDatasetFilterProvider existing = new DistributionLifecycleDatasetFilterProvider(
                new DistributionLifecycleStateFile(Path.of(STATE_FILE_PATH), null), null);
        dataset.setFilterProvider(existing);
        final DistributionLifecycleFilters filters = new RdfAbacDistributionLifecycleFilters();
        boolean installed = filters.installIfConfigured(dataset, APPLICATION_ID, STATE_FILE_PATH);

        assertFalse(installed, "Should not reinstall when a lifecycle filter is already present");
        assertSame(dataset.getFilterProvider(), existing,
                   "Existing lifecycle filter provider should be left untouched");
    }

    @Test
    public void installIfConfigured_returnsTrue_whenInstallingWithExplicitArguments() {
        final DistributionLifecycleFilters filters = new RdfAbacDistributionLifecycleFilters();
        boolean installed = filters.installIfConfigured(dataset, APPLICATION_ID, STATE_FILE_PATH);

        assertTrue(installed, "Should install when explicit lifecycle arguments are provided");
        assertTrue(dataset.getFilterProvider() instanceof DistributionLifecycleDatasetFilterProvider,
                   "Installed filter provider should be the lifecycle one");
    }

    @Test
    public void installIfConfigured_returnsTrue_andInstallsFreshFilter_whenStateFileConfigured() {
        assertNull(dataset.getFilterProvider(), "Pre-condition: dataset has no filter provider");
        final DistributionLifecycleFilters filters = new RdfAbacDistributionLifecycleFilters();
        boolean installed = filters.installIfConfigured(dataset, APPLICATION_ID, STATE_FILE_PATH);

        assertTrue(installed, "Should install when state file is configured");
        assertTrue(dataset.getFilterProvider() instanceof DistributionLifecycleDatasetFilterProvider,
                   "Installed filter provider should be the lifecycle one");
    }

    @Test
    public void installIfConfigured_returnsTrue_andWrapsExistingDelegate_whenStateFileConfigured() {
        DatasetFilterProvider existingDelegate = new DefaultDatasetFilterProvider();
        dataset.setFilterProvider(existingDelegate);
        final DistributionLifecycleFilters filters = new RdfAbacDistributionLifecycleFilters();
        boolean installed = filters.installIfConfigured(dataset, APPLICATION_ID, STATE_FILE_PATH);

        assertTrue(installed, "Should install (wrapping the existing delegate) when state file configured");
        assertTrue(dataset.getFilterProvider() instanceof DistributionLifecycleDatasetFilterProvider,
                   "Installed filter provider should be the lifecycle one");
    }

    @Test
    public void installIfConfigured_returnsTrue_whenApplicationIdIsNull() {
        final DistributionLifecycleFilters filters = new RdfAbacDistributionLifecycleFilters();
        boolean installed = filters.installIfConfigured(dataset, null, STATE_FILE_PATH);

        assertTrue(installed, "Application id is optional - install should still succeed");
        assertTrue(dataset.getFilterProvider() instanceof DistributionLifecycleDatasetFilterProvider);
    }

    @Test
    public void givenFilterProvider_whenUsing_thenAsExpected() {
        // Given
        final DistributionLifecycleDatasetFilterProvider provider =
                new DistributionLifecycleDatasetFilterProvider(mock(DistributionLifecycleStateFile.class),
                                                               null);
        DatasetGraph dsgPlain = DatasetGraphFactory.create();
        LabelsStore labelsStore = mock(LabelsStore.class);
        DatasetGraphABAC dsgABAC = new DatasetGraphABAC(dsgPlain, "*", labelsStore, Label.fromText("*"), mock(
                AttributesStore.class));
        CxtABAC context = mock(CxtABAC.class);

        // When
        Assert.assertTrue(
                provider.filterDataset(dsgPlain, labelsStore, null, context) instanceof DatasetGraphFilteredView);
        Assert.assertTrue(provider.filterDataset(dsgABAC, context) instanceof DatasetGraphFilteredView);


    }

    private static Path writeStateFile(String content) throws IOException {
        final Path stateFile = Files.createTempFile("distribution-lifecycle", ".json");
        stateFile.toFile().deleteOnExit();
        Files.writeString(stateFile, content, StandardCharsets.UTF_8);
        return stateFile;
    }

    private static Path activeAndWithdrawnStateFile() throws IOException {
        return writeStateFile("""
                              {
                                "distributions" : {
                                  "%s" : "Active",
                                  "%s" : "Withdrawn"
                                }
                              }
                              """.formatted(ACTIVE_GRAPH, WITHDRAWN_GRAPH));
    }

    private static DatasetGraph datasetWithBothGraphs() {
        final DatasetGraph dsg = DatasetGraphFactory.create();
        final Triple triple = Triple.create(NodeFactory.createURI("http://example/s"),
                                            NodeFactory.createURI("http://example/p"),
                                            NodeFactory.createURI("http://example/o"));
        dsg.add(Quad.create(NodeFactory.createURI(ACTIVE_GRAPH), triple));
        dsg.add(Quad.create(NodeFactory.createURI(WITHDRAWN_GRAPH), triple));
        dsg.add(Quad.create(Quad.defaultGraphIRI, triple));
        return dsg;
    }

    private static DatasetGraph lifecycleFilteredView(Path stateFile, DatasetGraph base) {
        final DistributionLifecycleDatasetFilterProvider provider =
                new DistributionLifecycleDatasetFilterProvider(new DistributionLifecycleStateFile(stateFile, null),
                                                               null);
        return provider.filterDataset(base, null, null, mock(CxtABAC.class));
    }

    private static long countQuads(Iterator<Quad> quads) {
        long count = 0;
        while (quads.hasNext()) {
            quads.next();
            count++;
        }
        return count;
    }

    /**
     * Regression test - a query that names the graph explicitly used to bypass the lifecycle filter entirely because
     * {@code DatasetGraphFilteredView} only applies its visible graph collection to graph enumeration.
     */
    @Test
    public void givenWithdrawnDistribution_whenQueryingItsNamedGraphDirectly_thenNoDataIsVisible() throws IOException {
        // Given
        final Path stateFile = activeAndWithdrawnStateFile();
        final DatasetGraph base = datasetWithBothGraphs();
        final Node withdrawn = NodeFactory.createURI(WITHDRAWN_GRAPH);

        // When
        final DatasetGraph filtered = lifecycleFilteredView(stateFile, base);

        // Then
        assertEquals(countQuads(filtered.find(withdrawn, Node.ANY, Node.ANY, Node.ANY)), 0L,
                     "Withdrawn distribution must not be reachable via a directly named graph");
        assertFalse(filtered.contains(withdrawn, Node.ANY, Node.ANY, Node.ANY),
                    "Withdrawn distribution must not be reachable via contains()");
        assertTrue(filtered.getGraph(withdrawn).isEmpty(),
                   "Withdrawn distribution's named graph must appear empty");

        // And the data is still physically stored
        assertFalse(base.getGraph(withdrawn).isEmpty(),
                    "Withdrawal must not delete the underlying named graph");
    }

    @Test
    public void givenActiveDistribution_whenQueryingItsNamedGraphDirectly_thenDataIsVisible() throws IOException {
        // Given
        final Path stateFile = activeAndWithdrawnStateFile();
        final DatasetGraph base = datasetWithBothGraphs();
        final Node active = NodeFactory.createURI(ACTIVE_GRAPH);

        // When
        final DatasetGraph filtered = lifecycleFilteredView(stateFile, base);

        // Then
        assertEquals(countQuads(filtered.find(active, Node.ANY, Node.ANY, Node.ANY)), 1L,
                     "Active distribution should be reachable via a directly named graph");
        assertFalse(filtered.getGraph(active).isEmpty(), "Active distribution's named graph should have data");
    }

    @Test
    public void givenLifecycleFilter_whenQueryingTheDefaultGraph_thenDataIsVisible() throws IOException {
        // Given
        final Path stateFile = activeAndWithdrawnStateFile();
        final DatasetGraph base = datasetWithBothGraphs();

        // When
        final DatasetGraph filtered = lifecycleFilteredView(stateFile, base);

        // Then - data that isn't in a distribution named graph isn't subject to distribution lifecycle
        assertFalse(filtered.getDefaultGraph().isEmpty(), "Default graph data should remain visible");
    }

    @Test
    public void givenWithdrawnDistribution_whenReactivated_thenDataBecomesVisibleAgain() throws IOException {
        // Given
        final Path stateFile = activeAndWithdrawnStateFile();
        final DatasetGraph base = datasetWithBothGraphs();
        final Node withdrawn = NodeFactory.createURI(WITHDRAWN_GRAPH);
        final DistributionLifecycleDatasetFilterProvider provider =
                new DistributionLifecycleDatasetFilterProvider(new DistributionLifecycleStateFile(stateFile, null),
                                                               null);
        assertTrue(provider.filterDataset(base, null, null, mock(CxtABAC.class)).getGraph(withdrawn).isEmpty(),
                   "Pre-condition: withdrawn distribution is hidden");

        // When - the running state store is updated, exactly as the lifecycle tracker does
        Files.writeString(stateFile, """
                                     {
                                       "distributions" : {
                                         "%s" : "Active",
                                         "%s" : "Active"
                                       }
                                     }
                                     """.formatted(ACTIVE_GRAPH, WITHDRAWN_GRAPH), StandardCharsets.UTF_8);

        // Then - a freshly derived per request view picks the change up without a restart
        assertFalse(provider.filterDataset(base, null, null, mock(CxtABAC.class)).getGraph(withdrawn).isEmpty(),
                    "Reactivated distribution should be queryable again");
    }

}
