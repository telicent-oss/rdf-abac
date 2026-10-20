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
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.VocabAuthz;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.security.data.distribution.DistributionLifecycleStateFile;
import io.telicent.smart.cache.security.data.labels.SecurityLabelsApplicator;
import io.telicent.smart.cache.security.data.plugins.AbstractDataSecurityPluginTests;
import io.telicent.smart.cache.security.data.plugins.DataSecurityPlugin;
import io.telicent.smart.caches.configuration.auth.UserInfo;
import io.telicent.smart.cache.security.data.requests.RequestContext;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.graph.GraphFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestRdfAbacPlugin extends AbstractDataSecurityPluginTests {

    @Override
    protected DataSecurityPlugin getPlugin() {
        return new RdfAbacPlugin();
    }

    @Override
    protected DatasetGraph createLabelledDataset(byte[] label) {
        final var labelsStore = mock(LabelsStore.class);
        when(labelsStore.labelForQuad(any())).thenReturn(new Label(label, StandardCharsets.UTF_8));
        return ABAC.authzDataset(DatasetGraphFactory.createTxnMem(), labelsStore, null, null);
    }

    @Override
    protected UserInfo getTestUserInfo(String username) {
        return UserInfo.builder()
                       .sub(username)
                       .attributes(Map.of("clearance", "S", "classification", "S", "org", "Telicent"))
                       .build();
    }

    public static byte[] labelBytes(String label) {
        return label.getBytes(StandardCharsets.UTF_8);
    }

    @DataProvider(name = "validLabels")
    @Override
    protected Object[][] validLabels() {
        return new Object[][] {
                { labelBytes("") },
                { labelBytes("clearance=S") },
                { labelBytes("clearance=S && (org=foo || org=bar)") },
                { labelBytes("clearance") },
                { labelBytes("clearance=S,(org=foo || org=bar)") },
                };
    }

    @DataProvider(name = "invalidLabels")
    @Override
    protected Object[][] invalidLabels() {
        return new Object[][] {
                { labelBytes("clearance=") }, { labelBytes("white space=bad") },
                };
    }

    @DataProvider(name = "accessibleLabels")
    @Override
    protected Object[][] accessibleLabels() {
        //@formatter:off
        return new Object[][] {
                { labelBytes("*") },
                { labelBytes("") },
                { labelBytes("clearance=S && org=Telicent") },
                { labelBytes("clearance=O") },
                { labelBytes("clearance=OS") },
                { labelBytes("classification=S")}
        };
        //@formatter:on
    }

    @DataProvider(name = "forbiddenLabels")
    @Override
    protected Object[][] forbiddenLabels() {
        return new Object[][] {
                { labelBytes("clearance=TS") },
                { labelBytes("classification=TS") },
                { labelBytes("org=Govt") }
        };
    }

    @Test
    public void givenDatasetWithLabelsGraph_whenPreparingApplicator_thenApplicatorReturned() {
        // Given
        DatasetGraph dsg = DatasetGraphFactory.create();
        Graph g = GraphFactory.createDefaultGraph();
        g.add(NodeFactory.createURI("https://example.org/s"), NodeFactory.createURI("https://example.org/p"),
              NodeFactory.createLiteralString("test"));
        dsg.addGraph(VocabAuthz.graphForLabels, g);

        // When
        try (SecurityLabelsApplicator applicator = this.getPlugin().prepareLabelsApplicator(null, dsg)) {
            // Then
            Assert.assertTrue(applicator instanceof RdfAbacApplicator);
        }
    }

    @Test
    public void givenNullUserAttribute_whenAuthorizing_thenValidAttributesRemainUsable() {
        Map<String, Object> attributes = new java.util.HashMap<>();
        attributes.put("org", "Telicent");
        attributes.put("clearance", null);
        RequestContext context = mock(RequestContext.class);
        when(context.userInfo()).thenReturn(UserInfo.builder().sub("test").attributes(attributes).build());
        DataSecurityPlugin plugin = getPlugin();

        try (var authorizer = plugin.prepareAuthorizer(context)) {
            Assert.assertTrue(authorizer.canRead(plugin.labelsParser().parseSecurityLabels(labelBytes("org=Telicent"))));
            Assert.assertFalse(authorizer.canRead(plugin.labelsParser().parseSecurityLabels(labelBytes("clearance=S"))));
        }
    }

    @Test
    public void givenPlugin_whenCheckingIfLabelsAreStringSafe_thenTrue() {
        // Given
        DataSecurityPlugin plugin = this.getPlugin();

        // When and Then
        Assert.assertTrue(plugin.areLabelsStringSafe());
    }

    @Test
    public void givenPlugin_whenUsingOptionalFeatures_thenPresent() {
        // Given
        DataSecurityPlugin plugin = this.getPlugin();

        // When and Then
        Assert.assertTrue(plugin.prepareDistributionLifecycleFilters().isPresent());
        Assert.assertTrue(plugin.prepareLabelsBackup(null).isEmpty());
        Assert.assertTrue(plugin.prepareLabelsRestore(null).isEmpty());
        Assert.assertTrue(plugin.prepareLabelsCompact(null).isEmpty());
        Assert.assertTrue(plugin.prepareLabelsRemover().isPresent());
        Assert.assertTrue(plugin.prepareLabelsModule().isPresent());
        Assert.assertNotNull(plugin.prepareLabelToNode());
        Assert.assertEquals(plugin.labelsGraphName().orElseThrow(), VocabAuthz.graphForLabels);
        Assert.assertTrue(plugin.prepareFusekiSink(null, true, null).isEmpty());
        Assert.assertTrue(plugin.prepareFusekiSink(mock(DatasetGraphABAC.class), true, mock(
                DistributionLifecycleStateFile.class)).isPresent());
        Assert.assertTrue(plugin.prepareLabelledDataset(null).isEmpty());
        Assert.assertTrue(plugin.prepareLabelledDataset(DatasetGraphFactory.empty()).isEmpty());
        Assert.assertTrue(plugin.prepareLabelledDataset(mock(DatasetGraphABAC.class)).isPresent());
        Assert.assertFalse(plugin.getReadOperations().isEmpty());
        Assert.assertFalse(plugin.getReadWriteOperations().isEmpty());
    }
}
