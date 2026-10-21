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

import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.VocabAuthz;
import io.telicent.jena.abac.labels.Label;
import io.telicent.smart.cache.security.data.DataSecurityException;
import io.telicent.smart.cache.security.data.labels.DatasetGraphLabelled;
import io.telicent.smart.cache.security.data.labels.SecurityLabels;
import io.telicent.smart.cache.security.data.labels.SecurityLabelsParser;
import io.telicent.smart.cache.storage.labels.LabelsStore;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraphWrapper;
import org.apache.jena.sparql.core.Quad;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

/**
 * A {@link DatasetGraphLabelled} implementation that adapts an RDF-ABAC {@link DatasetGraphABAC}
 * <p>
 * All data operations and transactions delegate to the wrapped dataset, as does label storage, so labels are written in
 * the same transaction as the data they label.
 * </p>
 */
public class RdfAbacDatasetGraphLabelled extends DatasetGraphWrapper implements DatasetGraphLabelled {

    private final DatasetGraphABAC abac;
    private final SecurityLabelsParser parser;

    /**
     * Creates a labelled dataset over a RDF-ABAC dataset
     *
     * @param abac   RDF-ABAC dataset
     * @param parser Labels parser to use
     */
    public RdfAbacDatasetGraphLabelled(DatasetGraphABAC abac, SecurityLabelsParser parser) {
        super(Objects.requireNonNull(abac, "abac cannot be null"));
        this.abac = abac;
        this.parser = Objects.requireNonNull(parser, "parser cannot be null");
    }

    @Override
    public Node labelsGraphName() {
        return VocabAuthz.graphForLabels;
    }

    @Override
    public SecurityLabelsParser labelsParser() {
        return this.parser;
    }

    @Override
    public void addLabels(Collection<Quad> quads, SecurityLabels<?> labels) {
        Objects.requireNonNull(quads, "quads cannot be null");
        Objects.requireNonNull(labels, "labels cannot be null");
        this.abac.labelsStore()
                 .addAll(new LinkedHashSet<>(quads),
                         Label.fromText(new String(labels.encoded(), StandardCharsets.UTF_8)));
    }

    @Override
    public void addLabelsGraph(Graph labels) {
        this.abac.labelsStore().addGraph(Objects.requireNonNull(labels, "labels cannot be null"));
    }

    @Override
    public void removeLabels(Quad quad) throws DataSecurityException {
        Objects.requireNonNull(quad, "quad cannot be null");
        try {
            // NB - The labels store is owned by the dataset and must stay open after removal
            this.abac.labelsStore().remove(quad);
        } catch (Exception e) {
            throw new DataSecurityException(e.getMessage(), e);
        }
    }

    @Override
    public Optional<LabelsStore> labelsStore() {
        return this.abac.labelsStore() instanceof LabelsStore store ? Optional.of(store) : Optional.empty();
    }


    @Override
    public Map<String, Long> labelsMetrics() {
        return this.abac.labelsStore().getMetrics();
    }

}
