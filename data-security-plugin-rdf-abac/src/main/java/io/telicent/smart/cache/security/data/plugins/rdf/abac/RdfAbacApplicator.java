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

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.security.data.labels.SecurityLabels;
import io.telicent.smart.cache.security.data.labels.SecurityLabelsApplicator;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * A labels applicator that uses an RDF-ABAC {@link LabelsStore} to decide what label applies to a given quad
 */
public class RdfAbacApplicator implements SecurityLabelsApplicator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RdfAbacApplicator.class);

    private final RdfAbacParser parser;
    private final LabelsStore labelsStore;
    private final SecurityLabels<?> defaultLabel;
    private final boolean ownsStore;

    /**
     * Creates a new applicator
     *
     * @param parser       Labels parser
     * @param defaultLabel Default label to apply if no more specific label applies, may be {@code null} if no default
     *                     should apply
     * @param labelsStore  Labels store containing triple/quad to label mappings
     * @param ownsStore    Whether the applicator owns the provided {@link LabelsStore} instance and should
     *                     {@link LabelsStore#close()} it in it's {@link #close()} method.
     */
    RdfAbacApplicator(RdfAbacParser parser, SecurityLabels<?> defaultLabel, LabelsStore labelsStore,
                      boolean ownsStore) {
        this.parser = Objects.requireNonNull(parser);
        this.defaultLabel = defaultLabel;
        this.labelsStore = Objects.requireNonNull(labelsStore);
        this.ownsStore = ownsStore;
    }

    @Override
    public SecurityLabels<?> defaultLabel() {
        return this.defaultLabel;
    }

    @Override
    public SecurityLabels<?> labelForTriple(Triple triple) {
        final Quad quad = Quad.create(Quad.defaultGraphIRI, triple);
        return labelForQuad(quad);
    }

    @Override
    public SecurityLabels<?> labelForQuad(Quad quad) {
        // LabelStore returns Label record which holds byte sequences plus charsets
        // For the encoded representation we simply copy the raw bytes for each label
        final Label label = this.labelsStore.labelForQuad(quad);
        if (label == null) {
            return this.defaultLabel;
        }
        return this.parser.parseSecurityLabels(label.data());
    }

    @Override
    public void close() {
        if (this.ownsStore) {
            try {
                this.labelsStore.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close Labels Store: ", e);
            }
        }
    }

}
