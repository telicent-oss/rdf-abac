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

import io.telicent.jena.abac.DefaultDatasetFilterProvider;
import io.telicent.jena.abac.DatasetFilterProvider;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.security.data.distribution.DistributionLifecycleStateFile;
import io.telicent.smart.cache.storage.rdf.DatasetGraphFilteredUnionView;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import org.apache.jena.sparql.core.Quad;

import java.util.Set;
import java.util.function.Predicate;

/**
 * A dataset filter provider that limits visible named graphs to distributions that are currently active.
 */
public class DistributionLifecycleDatasetFilterProvider implements DatasetFilterProvider {

    private static final DatasetFilterProvider DEFAULT_PROVIDER = new DefaultDatasetFilterProvider();
    private final DistributionLifecycleStateFile lifecycleStateFile;
    private final DatasetFilterProvider delegate;

    DistributionLifecycleDatasetFilterProvider(DistributionLifecycleStateFile lifecycleStateFile,
                                               DatasetFilterProvider delegate) {
        this.lifecycleStateFile = lifecycleStateFile;
        this.delegate = delegate != null ? delegate : DEFAULT_PROVIDER;
    }

    @Override
    public DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
        final DatasetGraph filtered = this.delegate.filterDataset(dsgAuthz, cxt);
        return applyLifecycleFilter(filtered);
    }

    @Override
    public DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, Label defaultLabel, CxtABAC cxt) {
        final DatasetGraph filtered = this.delegate.filterDataset(dsgBase, labels, defaultLabel, cxt);
        return applyLifecycleFilter(filtered);
    }

    private DatasetGraph applyLifecycleFilter(DatasetGraph dataset) {
        final Set<Node> activeGraphs = this.lifecycleStateFile.activeGraphNodes();
        // IMPORTANT - We use our DatasetGraphFilteredUnionView in preference to the Jena DatasetGraphFilteredView.  The
        //             Jena implementation does not handle union default graph mode correctly on all code paths which
        //             can cause queries to return no results when results should be expected.
        return new DatasetGraphFilteredUnionView(dataset, lifecycleQuadFilter(activeGraphs), activeGraphs);
    }

    /**
     * Builds the quad level filter that hides quads belonging to named graphs for distributions that are not
     * currently {@code Active}.
     * <p>
     * The {@code visibleGraphs} collection passed to {@link DatasetGraphFilteredView} only constrains graph
     * <strong>enumeration</strong> i.e. {@code listGraphNodes()} and the union graph.  A query that names a graph
     * explicitly, e.g. {@code ASK { GRAPH <distribution-uri> { ?s ?p ?o } }}, reaches the underlying data via
     * {@code find(g, s, p, o)} which is only constrained by the quad filter.  Supplying {@code null} as the quad
     * filter therefore left a hole through which a withdrawn distribution remained directly queryable, so we always
     * supply a filter that enforces the same policy at the quad level.
     * </p>
     *
     * @param activeGraphs Named graphs for the distributions that are currently active
     * @return Quad filter, never {@code null}
     */
    private static Predicate<Quad> lifecycleQuadFilter(Set<Node> activeGraphs) {
        return quad -> isVisible(quad, activeGraphs);
    }

    private static boolean isVisible(Quad quad, Set<Node> activeGraphs) {
        if (quad == null) {
            return false;
        }
        final Node graph = quad.getGraph();
        if (graph == null || Quad.isDefaultGraph(graph) || Quad.isUnionGraph(graph)) {
            // Data that isn't in a distribution named graph isn't subject to distribution lifecycle
            return true;
        }
        return activeGraphs.contains(graph);
    }

}
