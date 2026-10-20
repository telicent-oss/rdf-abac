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
package io.telicent.smart.cache.security.data.plugins.rdf.abac.utils;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import lombok.Generated;
import lombok.experimental.Delegate;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * A decorator for {@link LabelsStore} that returns a fallback default label
 */
@Generated
public class DefaultingLabelsStore implements LabelsStore {
    private final @Delegate LabelsStore store;
    private final Label defaultLabel;

    /**
     * Creates a new labels store with a fallback default label
     *
     * @param labelStore    Actual labels store
     * @param defaultLabel Fallback default label
     */
    public DefaultingLabelsStore(final LabelsStore labelStore, final byte[] defaultLabel) {
        Objects.requireNonNull(labelStore);
        if (isBlank(defaultLabel)) {
            throw new IllegalArgumentException("Default Label cannot be blank");
        }
        this.store = labelStore;
        this.defaultLabel = new Label(defaultLabel, StandardCharsets.UTF_8);
    }

    private boolean isBlank(byte[] defaultLabels) {
        if (defaultLabels == null) {
            return true;
        }
        for (byte label : defaultLabels) {
            if (label != 0) {
                return false;
            }
        }
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public Label labelForTriple(final Triple triple) {
        final Quad quad = Quad.create(Quad.defaultGraphIRI, triple);
        final Label label = this.store.labelForQuad(quad);
        return label == null ? this.defaultLabel : label;
    }

    @Override
    public Label labelForQuad(final Quad quad) {
        final Label label = this.store.labelForQuad(quad);
        return label == null ? this.defaultLabel : label;
    }

}

