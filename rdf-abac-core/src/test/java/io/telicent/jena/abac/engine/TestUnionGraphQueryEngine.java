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

package io.telicent.jena.abac.engine;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.Labels;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.QueryExecDatasetBuilder;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestUnionGraphQueryEngine {

    private static final String DEFAULT_GRAPH_QUERY = "SELECT * WHERE { ?s ?p ?o }";

    private DatasetGraphABAC buildABACDataset() {
        final DatasetGraph base = DatasetGraphFactory.createTxnMem();
        final AttributesStore attributesStore = Mockito.mock(AttributesStore.class);
        return ABAC.authzDataset(base, null, Labels.emptyStore(), Label.fromText("*"), attributesStore);
    }

    /** Populate base with two named-graph triples and one default-graph triple. */
    private void populateDataset(DatasetGraph base) {
        Txn.executeWrite(base, () -> {
            base.add(NodeFactory.createURI("http://example.org/graph#1"),
                     NodeFactory.createURI("http://example.org/subject"),
                     NodeFactory.createURI("http://example.org/predicate"),
                     NodeFactory.createLiteralString("from-graph-1"));
            base.add(NodeFactory.createURI("http://example.org/graph#2"),
                     NodeFactory.createURI("http://example.org/subject"),
                     NodeFactory.createURI("http://example.org/predicate"),
                     NodeFactory.createLiteralString("from-graph-2"));
            base.add(Quad.defaultGraphIRI,
                     NodeFactory.createURI("http://example.org/subject"),
                     NodeFactory.createURI("http://exampke.org/predicate"),
                     NodeFactory.createLiteralString("from-default-graph"));
        });
    }

    private long countDefaultGraphResults(DatasetGraph dsg) {
        try (QueryExec qe = QueryExecDatasetBuilder.create()
                .dataset(dsg)
                .query(DEFAULT_GRAPH_QUERY)
                .build()) {
            long count = 0;
            var rowSet = qe.select();
            while (rowSet.hasNext()) {
                rowSet.next();
                count++;
            }
            return count;
        }
    }

    @BeforeEach
    void setup() {
        UnionGraphQueryEngine.routingCheck = () -> true;
        UnionGraphQueryEngine.register();
    }

    @AfterEach
    void teardown() {
        UnionGraphQueryEngine.unregister();
        // Restore the real env-var reader
        UnionGraphQueryEngine.routingCheck =
                () -> Boolean.parseBoolean(System.getenv(UnionGraphQueryEngine.ENV_ROUTE_TO_NAMED_GRAPHS));
    }

    // ---- accept() / registration tests

    @Test
    void givenEnvVarTrue_whenCheckingQueryAccept_thenFactoryAcceptsFilteredView() {
        final DatasetGraph filteredView = new DatasetGraphFilteredView(
                DatasetGraphFactory.createTxnMem(), null, List.of());
        final QueryEngineFactory factory = UnionGraphQueryEngine.getFactory();
        assertTrue(factory.accept((Query) null, filteredView, null));
    }

    @Test
    void givenEnvVarTrue_whenCheckingOpAccept_thenFactoryAcceptsABACDataset() {
        final QueryEngineFactory factory = UnionGraphQueryEngine.getFactory();
        assertTrue(factory.accept((Op) null, buildABACDataset(), null));
    }

    @Test
    void givenEnvVarFalse_whenCheckingAccept_thenFactoryRejectsABACDataset() {
        UnionGraphQueryEngine.routingCheck = () -> false;
        final QueryEngineFactory factory = UnionGraphQueryEngine.getFactory();
        assertFalse(factory.accept((Query) null, buildABACDataset(), null));
        assertFalse(factory.accept((Op) null, buildABACDataset(), null));
    }

    @Test
    void givenEnvVarTrue_whenCheckingAccept_thenFactoryRejectsNonABACDataset() {
        final QueryEngineFactory factory = UnionGraphQueryEngine.getFactory();
        assertFalse(factory.accept((Query) null, DatasetGraphZero.create(), null));
    }

    @Test
    void givenRegisteredEngine_whenCheckingRegistry_thenFactoryIsPresent() {
        assertTrue(QueryEngineRegistry.containsFactory(UnionGraphQueryEngine.getFactory()));
    }

    @Test
    void givenEngineRegisteredTwice_whenCheckingRegistry_thenOnlyOneEntryExists() {
        UnionGraphQueryEngine.register();
        UnionGraphQueryEngine.register();
        final long count = QueryEngineRegistry.get().factories().stream()
                .filter(f -> f == UnionGraphQueryEngine.getFactory())
                .count();
        assertEquals(1, count);
    }

    @Test
    void givenUnregisteredEngine_whenCheckingRegistry_thenFactoryIsAbsent() {
        UnionGraphQueryEngine.unregister();
        assertFalse(QueryEngineRegistry.containsFactory(UnionGraphQueryEngine.getFactory()));
    }

    // ---- Functional tests

    @Test
    void givenEnvVarTrue_whenQueryingDefaultGraph_thenUnionOfNamedGraphsIsReturned() {
        // Given
        final DatasetGraphABAC dsg = buildABACDataset();
        final DatasetGraph base = dsg.getData();
        populateDataset(base);

        // Simulate the filtered view produced after ABAC security evaluation,
        // exposing the two named graphs with no additional quad filter.
        final DatasetGraph filteredView = new DatasetGraphFilteredView(base, null,
                List.of(NodeFactory.createURI("http://example.org/graph#1"),
                        NodeFactory.createURI("http://example.org/graph#2")));

        // When / Then: sees from-g1 + from-g2 (named graph union), not from-default
        assertEquals(2, countDefaultGraphResults(filteredView));
    }

    @Test
    void givenEnvVarFalse_whenQueryingDefaultGraph_thenOnlyDefaultGraphIsReturned() {
        // Given: env var off — factory declines, QueryEngineMain handles the query
        UnionGraphQueryEngine.routingCheck = () -> false;
        final DatasetGraphABAC dsg = buildABACDataset();
        populateDataset(dsg.getData());

        // When / Then: standard engine sees only the default graph triple
        assertEquals(1, countDefaultGraphResults(dsg));
    }
}
