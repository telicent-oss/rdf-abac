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

package io.telicent.jena.abac;

import io.telicent.jena.abac.assembler.Secured;
import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.Labels;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static io.telicent.jena.abac.TestAllNamedGraphs.addNamedGraphs;
import static io.telicent.jena.abac.TestAllNamedGraphs.graphName;
import static io.telicent.jena.abac.TestAssemblerABAC.assemble;
import static io.telicent.jena.abac.TestAssemblerABAC.assembleBad;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@code authz:visibleGraphsFile} feature, which loads the set of
 * visible named graphs from a plain-text file.
 */
public class TestVisibleNamedGraphsFile {

    static {
        JenaSystem.init();
    }

    private static final String TEST_DIR = "src/test/files/dataset/";

    @BeforeAll
    public static void beforeAll() {
        Labels.rocks.clear();
        LogCtl.set(Secured.BUILD_LOG, "error");
    }

    @AfterEach
    public void afterEach() {
        Labels.rocks.clear();
    }

    // ---- Assembler configuration tests

    @Test
    public void givenAssemblerWithNoVisibleGraphsFile_whenAssembled_thenVisibleNamedGraphsIsNull() {
        // Given / When
        final DatasetGraphABAC dsg = assemble(TEST_DIR + "abac-assembler-1.ttl");

        // Then (null means all named graphs are visible)
        assertNull(dsg.getVisibleNamedGraphs());
        dsg.close();
    }

    @Test
    public void givenAssemblerWithValidVisibleGraphsFile_whenAssembled_thenListedGraphsAreLoaded() {
        // Given / When
        final DatasetGraphABAC dsg = assemble(TEST_DIR + "abac-assembler-visible-graphs.ttl");

        //Then
        final Collection<Node> allowed = dsg.getVisibleNamedGraphs();
        assertNotNull(allowed);
        assertEquals(2, allowed.size());
        assertTrue(allowed.contains(NodeFactory.createURI("http://telicent.io/graph#1")));
        assertTrue(allowed.contains(NodeFactory.createURI("http://telicent.io/graph#2")));
        dsg.close();
    }

    @Test
    public void givenAssemblerWithMissingVisibleGraphsFile_thenAssemblerExceptionIsThrown() {
        assembleBad(TEST_DIR + "abac-assembler-visible-graphs-bad.ttl");
    }

    // ---- Filter behaviour tests ----

    @Test
    public void givenDatasetAbacWithNullVisibleGraphs_whenFiltered_thenAllNamedGraphsAreVisible() {
        // Given
        final DatasetGraph dsgBase = DatasetGraphFactory.create();
        addNamedGraphs(dsgBase, 3);
        final DatasetGraphABAC dsgAuthz = ABAC.authzDataset(dsgBase, null, null, null,
                Mockito.mock(AttributesStore.class), null);

        // When
        final DatasetGraph filtered = ABAC.filterDataset(dsgAuthz, Mockito.mock(CxtABAC.class));

        // Then
        final List<Node> visible = listGraphNodes(filtered);
        assertEquals(3, visible.size());
        for (int i = 1; i <= 3; i++) {
            assertTrue(visible.contains(graphName(i)), "Expected graph " + i + " to be visible");
        }
    }

    @Test
    public void givenDatasetAbacWithRestrictedVisibleGraphs_whenFiltered_thenOnlyVisibleNamedGraphsAreVisible() {
        // Given
        final DatasetGraph dsgBase = DatasetGraphFactory.create();
        addNamedGraphs(dsgBase, 3);
        final Set<Node> visibleGraphs = Set.of(graphName(1), graphName(2));
        final DatasetGraphABAC dsgAuthz = ABAC.authzDataset(dsgBase, null, null, null,
                Mockito.mock(AttributesStore.class), visibleGraphs);

        // When
        final DatasetGraph filtered = ABAC.filterDataset(dsgAuthz, Mockito.mock(CxtABAC.class));

        // Then
        final List<Node> visible = listGraphNodes(filtered);
        assertEquals(2, visible.size());
        assertTrue(visible.contains(graphName(1)));
        assertTrue(visible.contains(graphName(2)));
        assertFalse(visible.contains(graphName(3)));
    }

    @Test
    public void givenDatasetAbacWithEmptyVisibleGraphs_whenFiltered_thenNoNamedGraphsAreVisible() {
        // Given
        final DatasetGraph dsgBase = DatasetGraphFactory.create();
        addNamedGraphs(dsgBase, 3);
        final DatasetGraphABAC dsgAuthz = ABAC.authzDataset(dsgBase, null, null, null,
                Mockito.mock(AttributesStore.class), Set.of());

        // When
        final DatasetGraph filtered = ABAC.filterDataset(dsgAuthz, Mockito.mock(CxtABAC.class));

        // Then (an empty set of visible graphs means the visible graphs file is available but no graphs are specified)
        assertTrue(listGraphNodes(filtered).isEmpty());
    }

    private static List<Node> listGraphNodes(DatasetGraph dsg) {
        final List<Node> nodes = new ArrayList<>();
        dsg.listGraphNodes().forEachRemaining(nodes::add);
        return nodes;
    }
}
