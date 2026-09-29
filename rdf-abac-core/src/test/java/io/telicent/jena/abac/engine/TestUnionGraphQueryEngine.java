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
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.attributes.syntax.AEX;
import io.telicent.jena.abac.core.*;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsGetter;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.storage.rdf.DatasetGraphFilteredUnionView;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.Query;
import org.apache.jena.rdfpatch.RDFChanges;
import org.apache.jena.rdfpatch.system.DatasetGraphChanges;
import org.apache.jena.sparql.algebra.Op;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.engine.Plan;
import org.apache.jena.sparql.engine.QueryEngineFactory;
import org.apache.jena.sparql.engine.QueryEngineRegistry;
import org.apache.jena.sparql.engine.binding.Binding;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.exec.QueryExecDatasetBuilder;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.system.Txn;
import org.apache.jena.system.buffering.BufferingDatasetGraph;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SuppressWarnings("java:S5786")
public class TestUnionGraphQueryEngine {

    private static final String DEFAULT_GRAPH_QUERY = "SELECT * WHERE { ?s ?p ?o }";

    private DatasetGraphABAC buildABACDataset() {
        final AttributesStore attributesStore = mock(AttributesStore.class);
        return buildABACDataset(Labels.emptyStore(), AEX.strALLOW, attributesStore);
    }

    private DatasetGraphABAC buildABACDataset(LabelsStore labels, String defaultLabel,
                                              AttributesStore attributesStore) {
        final DatasetGraph base = DatasetGraphFactory.createTxnMem();
        return ABAC.authzDataset(base, null, labels, Label.fromText(defaultLabel), attributesStore);
    }

    protected static final org.apache.jena.graph.Node NG1 = NodeFactory.createURI("http://example.org/graph#1");
    private static final Quad QUAD_NG1 = Quad.create(NG1, NodeFactory.createURI("http://example.org/subject"),
                                                     NodeFactory.createURI("http://example.org/predicate"),
                                                     NodeFactory.createLiteralString("from-graph-1"));
    protected static final org.apache.jena.graph.Node NG2 = NodeFactory.createURI("http://example.org/graph#2");
    private static final Quad QUAD_NG2 = Quad.create(NG2, NodeFactory.createURI("http://example.org/subject"),
                                                     NodeFactory.createURI("http://example.org/predicate"),
                                                     NodeFactory.createLiteralString("from-graph-2"));
    private static final Quad QUAD_DEFAULT_GRAPH =
            Quad.create(Quad.defaultGraphIRI, NodeFactory.createURI("http://example.org/subject"),
                        NodeFactory.createURI("http://exampke.org/predicate"),
                        NodeFactory.createLiteralString("from-default-graph"));

    /**
     * Populate base with two named-graph triples and one default-graph triple.
     */
    private void populateDataset(DatasetGraph base) {
        Txn.executeWrite(base, () -> {
            base.add(QUAD_NG1);
            base.add(QUAD_NG2);
            base.add(QUAD_DEFAULT_GRAPH);
        });
    }

    /**
     * Places a {@link DatasetGraphFilteredView} wrapper around the base dataset as ABAC filtering would normally do
     *
     * @param base   Base dataset
     * @param filter Security filter
     * @return Filtered view
     */
    private static DatasetGraphFilteredView createFilteredView(DatasetGraph base, QuadFilter filter) {
        return new DatasetGraphFilteredView(base, filter, List.of(NG1, NG2));
    }

    private long countDefaultGraphResults(DatasetGraph dsg) {
        try (QueryExec qe = QueryExecDatasetBuilder.create().dataset(dsg).query(DEFAULT_GRAPH_QUERY).build()) {
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
        final DatasetGraph filteredView =
                new DatasetGraphFilteredView(DatasetGraphFactory.createTxnMem(), null, List.of());
        final QueryEngineFactory factory = UnionGraphQueryEngine.getFactory();
        assertTrue(factory.accept((Query) null, filteredView, null));
    }

    @Test
    void givenEnvVarTrue_whenCheckingOpAccept_thenFactoryAcceptsFilteredView() {
        final DatasetGraph filteredView =
                new DatasetGraphFilteredView(DatasetGraphFactory.createTxnMem(), null, List.of());
        final QueryEngineFactory factory = UnionGraphQueryEngine.getFactory();
        assertTrue(factory.accept((Op) null, filteredView, null));
    }

    @Test
    void givenEnvVarFalse_whenCheckingAccept_thenFactoryRejectsFilteredView() {
        final DatasetGraph filteredView =
                new DatasetGraphFilteredView(DatasetGraphFactory.createTxnMem(), null, List.of());
        UnionGraphQueryEngine.routingCheck = () -> false;
        final QueryEngineFactory factory = UnionGraphQueryEngine.getFactory();
        assertFalse(factory.accept((Query) null, filteredView, null));
        assertFalse(factory.accept((Op) null, filteredView, null));
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
        final long count = QueryEngineRegistry.get()
                                              .factories()
                                              .stream()
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
        final DatasetGraph filteredView = createFilteredView(base, null);

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

    // ---- Complex Functional Tests
    //      Tests for interaction between this and other systems, in particular how different layers of DatasetGraph
    //      wrapping affect QueryEngineFactory behaviour and union default graph mode

    /**
     * Creates an in-memory labels store with some labels for each of our test quads
     *
     * @return Labels store
     */
    private static LabelsStore createLabelsStore() {
        LabelsStore labels = Labels.createLabelsStoreMem();
        labels.add(QUAD_DEFAULT_GRAPH, Label.fromText("employee"));
        labels.add(QUAD_NG1, Label.fromText("employee"));
        labels.add(QUAD_NG2, Label.fromText("employee || contractor"));
        return labels;
    }

    /**
     * Creates an in-memory attributes store with attributes for each of our test users
     *
     * @return Attributes store
     */
    private static AttributesStoreLocal createAttributesStore() {
        AttributesStoreLocal attributes = new AttributesStoreLocal();
        attributes.put("u1", AttributeValueSet.of(AttributeValue.of("employee", ValueTerm.TRUE)));
        attributes.put("u2", AttributeValueSet.of(AttributeValue.of("contractor", ValueTerm.TRUE)));
        return attributes;
    }

    /**
     * Creates an ABAC security filter for a given user
     *
     * @param getter     Label Getter
     * @param dsg        ABAC dataset
     * @param attributes Attributes Store
     * @param user       User
     * @param base       Base dataset
     * @return Security label filter
     */
    private static QuadFilter filterForUser(LabelsGetter getter, DatasetGraphABAC dsg, AttributesStoreLocal attributes,
                                            String user, DatasetGraph base) {
        AttributeValueSet userAttributes = attributes.attributes(user);
        return Labels.securityFilterByLabel(getter, dsg.getDefaultLabel(), CxtABAC.context(
                userAttributes != null ? userAttributes : AttributeValueSet.EMPTY, attributes, base));
    }

    @Test
    void givenAbacFiltering_whenQueryingUnionDefaultGraph_thenAccessibleTriplesReturned() {
        // Given
        LabelsStore labels = createLabelsStore();
        AttributesStoreLocal attributes = createAttributesStore();
        final DatasetGraphABAC dsg = buildABACDataset(labels, AEX.strDENY, attributes);
        final DatasetGraph base = dsg.getData();
        populateDataset(base);
        LabelsGetter getter = labels::labelForQuad;
        QuadFilter filterU1 = filterForUser(getter, dsg, attributes, "u1", base);
        QuadFilter filterU2 = filterForUser(getter, dsg, attributes, "u2", base);
        QuadFilter filterU3 = filterForUser(getter, dsg, attributes, "u3", base);

        // When
        final DatasetGraph filteredViewU1 = createFilteredView(base, filterU1);
        final DatasetGraph filteredViewU2 = createFilteredView(base, filterU2);
        final DatasetGraph filteredViewU3 = createFilteredView(base, filterU3);

        // Then
        assertEquals(2, countDefaultGraphResults(filteredViewU1));
        assertEquals(1, countDefaultGraphResults(filteredViewU2));
        assertEquals(0, countDefaultGraphResults(filteredViewU3));
    }

    @Test
    void givenAbacFilteringPlusAdditionalWrapping_whenQueryingUnionDefaultGraph_thenAccessibleTriplesReturned() {
        // Given
        LabelsStore labels = createLabelsStore();
        AttributesStoreLocal attributes = createAttributesStore();
        final DatasetGraphABAC dsg = buildABACDataset(labels, AEX.strDENY, attributes);
        final DatasetGraph base = dsg.getData();
        populateDataset(base);
        LabelsGetter getter = labels::labelForQuad;
        QuadFilter filterU1 = filterForUser(getter, dsg, attributes, "u1", base);
        QuadFilter filterU2 = filterForUser(getter, dsg, attributes, "u2", base);
        RDFChanges changes = mock(RDFChanges.class);

        // When
        final DatasetGraph filteredViewU1 = createFilteredView(base, filterU1);
        final DatasetGraph filteredViewU2 = createFilteredView(base, filterU2);

        // Then
        assertEquals(2, countDefaultGraphResults(new DatasetGraphChanges(filteredViewU1, changes)));
        assertEquals(1, countDefaultGraphResults(new DatasetGraphChanges(filteredViewU2, changes)));
    }

    /**
     * Places a {@link DatasetGraphFilteredUnionView} wrapper around the base dataset as ABAC filtering would normally
     * do
     *
     * @param base   Base dataset
     * @param filter Security filter
     * @return Filtered view
     */
    private static DatasetGraphFilteredUnionView createFilteredUnionView(DatasetGraph base, QuadFilter filter) {
        return new DatasetGraphFilteredUnionView(base, filter, List.of(NG1, NG2));
    }

    /**
     * Applies a more complex wrapping to the given dataset
     * <p>
     * More specifically this wraps it in a {@link BufferingDatasetGraph} which is not a
     * {@link org.apache.jena.sparql.core.DatasetGraphWrapper}.  This causes our {@link UnionGraphQueryEngine} not to
     * kick in directly, or via Jena's {@link org.apache.jena.sparql.engine.QueryEngineFactoryWrapper} which unwraps
     * things to determine the right query engine to use.  As a result when this wrapping is applied the union default
     * graph mode can fail to function due to how various {@code find()} calls are routed through the wrapped dataset
     * hierarchy.  Therefore, the choice of the dataset to wrap can impact behaviour hence why our
     * {@link DatasetGraphFilteredUnionView} exists.
     * </p>
     *
     * @param dataset Dataset
     * @param changes RDF Changes
     * @return Complex wrapped dataset
     */
    private static DatasetGraph complexWrapping(DatasetGraph dataset, RDFChanges changes) {
        return new DatasetGraphChanges(new BufferingDatasetGraph(dataset), changes);
    }

    @Test
    void givenAbacFilteringPlusComplexWrapping_whenQueryingRealDefaultGraph_thenOnlyRealDefaultGraphTriplesSeen() {
        // Given
        LabelsStore labels = createLabelsStore();
        AttributesStoreLocal attributes = createAttributesStore();
        final DatasetGraphABAC dsg = buildABACDataset(labels, AEX.strDENY, attributes);
        final DatasetGraph base = dsg.getData();
        populateDataset(base);
        LabelsGetter getter = labels::labelForQuad;
        QuadFilter filterU1 = filterForUser(getter, dsg, attributes, "u1", base);
        QuadFilter filterU2 = filterForUser(getter, dsg, attributes, "u2", base);
        RDFChanges changes = mock(RDFChanges.class);

        // When
        final DatasetGraph filteredViewU1 = createFilteredUnionView(base, filterU1);
        final DatasetGraph filteredViewU2 = createFilteredUnionView(base, filterU2);

        // Then
        // NB - Due to the layers of wrapping here our UnionQueryEngineFactory does not recognise this as an acceptable
        //      dataset and so doesn't apply.  Thus, the query only queries the real default graph and not the union
        //      default graph.  As a result expected counts are lower.
        assertEquals(1, countDefaultGraphResults(complexWrapping(filteredViewU1, changes)));
        assertEquals(0, countDefaultGraphResults(complexWrapping(filteredViewU2, changes)));
    }

    @Test
    void givenAbacFilteringPlusComplexWrappingAndCustomQueryEngine_whenQueryingUnionDefaultGraph_thenAccessibleTriplesReturned() {
        // Given
        LabelsStore labels = createLabelsStore();
        AttributesStoreLocal attributes = createAttributesStore();
        final DatasetGraphABAC dsg = buildABACDataset(labels, AEX.strDENY, attributes);
        final DatasetGraph base = dsg.getData();
        populateDataset(base);
        LabelsGetter getter = labels::labelForQuad;
        QuadFilter filterU1 = filterForUser(getter, dsg, attributes, "u1", base);
        QuadFilter filterU2 = filterForUser(getter, dsg, attributes, "u2", base);
        RDFChanges changes = mock(RDFChanges.class);
        QueryEngineFactory customQueryEngineFactory = new CustomQueryEngineFactory();
        try {
            QueryEngineRegistry.addFactory(customQueryEngineFactory);

            // When
            final DatasetGraph filteredViewU1 = createFilteredUnionView(base, filterU1);
            final DatasetGraph filteredViewU2 = createFilteredUnionView(base, filterU2);

            // Then
            // Here the custom query engine factory does accept this complex wrapping so the union default graph is used
            // and applies correctly so counts reflect the users expected union default graph views of the data
            assertEquals(2, countDefaultGraphResults(complexWrapping(filteredViewU1, changes)));
            assertEquals(1, countDefaultGraphResults(complexWrapping(filteredViewU2, changes)));
        } finally {
            QueryEngineRegistry.removeFactory(customQueryEngineFactory);
        }
    }

    @Test
    void givenAbacFilteringPlusComplexWrappingAndCustomQueryEngine_whenQueryingUnionDefaultGraphWithoutUnionView_thenNoTriplesReturned() {
        // Given
        LabelsStore labels = createLabelsStore();
        AttributesStoreLocal attributes = createAttributesStore();
        final DatasetGraphABAC dsg = buildABACDataset(labels, AEX.strDENY, attributes);
        final DatasetGraph base = dsg.getData();
        populateDataset(base);
        LabelsGetter getter = labels::labelForQuad;
        QuadFilter filterU1 = filterForUser(getter, dsg, attributes, "u1", base);
        QuadFilter filterU2 = filterForUser(getter, dsg, attributes, "u2", base);
        RDFChanges changes = mock(RDFChanges.class);
        QueryEngineFactory customQueryEngineFactory = new CustomQueryEngineFactory();
        try {
            QueryEngineRegistry.addFactory(customQueryEngineFactory);

            // When
            final DatasetGraph filteredViewU1 = createFilteredView(base, filterU1);
            final DatasetGraph filteredViewU2 = createFilteredView(base, filterU2);

            // Then
            // Due to limitations in how DatasetGraphFilteredView handles union default graph mode when using it
            // directly with complex wrapping queries can return no results.  This is because the quad filter sees the
            // "wrong" graph on the quads and so the filter falls back to the default label, which is !, and denies
            // access.
            assertEquals(0, countDefaultGraphResults(complexWrapping(filteredViewU1, changes)));
            assertEquals(0, countDefaultGraphResults(complexWrapping(filteredViewU2, changes)));
        } finally {
            QueryEngineRegistry.removeFactory(customQueryEngineFactory);
        }
    }

    public static final class CustomQueryEngineFactory implements QueryEngineFactory {

        @Override
        public boolean accept(Query query, DatasetGraph dataset, Context context) {
            return dataset instanceof DatasetGraphChanges;
        }

        @Override
        public Plan create(Query query, DatasetGraph dataset, Binding inputBinding, Context context) {
            return new UnionGraphQueryEngine(query, dataset, inputBinding, context).getPlan();
        }

        @Override
        public boolean accept(Op op, DatasetGraph dataset, Context context) {
            return dataset instanceof DatasetGraphChanges;
        }

        @Override
        public Plan create(Op op, DatasetGraph dataset, Binding inputBinding, Context context) {
            return new UnionGraphQueryEngine(op, dataset, inputBinding, context).getPlan();
        }
    }
}
