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

package io.telicent.jena.abac.labels;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.system.Txn;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * {@link LabelsStore}s provide a quad to label mapping.
 */
public interface LabelsStore extends AutoCloseable {

    /**
     * Lookup the triple and return the label associated with it.
     * <p>
     * Implementation <strong>MUST</strong> assume that the triple is in the default graph.
     * </p>
     *
     * @param triple Triple to lookup
     * @return Label, or {@code null} if no label for triple
     * @deprecated Use {@link #labelForQuad(Quad)}
     */
    @Deprecated
    default Label labelForTriple(Triple triple) {
        return labelForQuad(Quad.create(Quad.defaultGraphIRI, triple));
    }

    /**
     * Lookup the quad and return the label associated with it
     *
     * @param quad Quad
     * @return Label, or {@code null} if no label for quad
     */
    Label labelForQuad(Quad quad);

    /**
     * A {@link Transactional} that protects the label store.
     */
    Transactional getTransactional();

    /**
     * A concrete or pattern triple
     * <p>
     * Implementation <strong>MUST</strong> assume that the triple is in the default graph.
     * </p>
     *
     * @deprecated Use {@link #add(Quad, Label)}
     */
    @Deprecated
    default void add(Triple triple, Label label) {
        add(Quad.create(Quad.defaultGraphIRI, triple), label);
    }

    /**
     * Labels for a specific triple.
     * <p>
     * Implementation <strong>MUST</strong> assume that the triple is in the default graph.
     * </p>
     *
     * @deprecated Use {@link #add(Node, Node, Node, Node, Label)}
     */
    @Deprecated
    default void add(Node subject, Node property, Node object, Label label) {
        add(Triple.create(subject, property, object), label);
    }

    /**
     * Adds a label for a specific quad
     *
     * @param quad  Quad
     * @param label Label to apply
     */
    void add(Quad quad, Label label);

    /**
     * Adds a label for a specific quad
     */
    default void add(Node graph, Node subject, Node property, Node object, Label label) {
        add(Quad.create(graph, subject, property, object), label);
    }

    /**
     * Add a graph of label descriptions to the store. This is done inside a transaction.
     *
     * @param labelsData Labels graph
     * @see L#loadStoreFromGraph
     */
    default void addGraph(Graph labelsData) {
        Txn.executeWrite(getTransactional(), () -> L.loadStoreFromGraph(this, labelsData));
    }

    /**
     * Remove the label for a specific triple.
     * <p>
     * Implementation <strong>MUST</strong> assume that the triple is in the default graph.
     * </p>
     *
     * @param triple Triple
     * @deprecated Use {@link #remove(Quad)}
     */
    @Deprecated
    default void remove(Triple triple) {
        remove(Quad.create(Quad.defaultGraphIRI, triple));
    }

    /**
     * Remove the label for a specific quad.
     *
     * @param quad Quad
     */
    void remove(Quad quad);

    /**
     * Is the store empty?
     */
    boolean isEmpty();

    /**
     * Apply BiConsumer to each entry in the labels store.
     */
    void forEach(BiConsumer<Quad, Label> action);

    /**
     * Get labels as graph. This is a development and deployment helper; it may not be supported by all store
     * implementations The graph may be very large. Returns a copy of the labels graph, so it is not connected to the
     * LabelsStore. Returns null if not supported.
     */
    Graph asGraph();

    /**
     * A collection of implementation-dependent values which may be used when testing a {@code LabelStore}
     * implementation.
     *
     * @return the store properties for this labels store implementation
     */
    Map<String, String> getProperties();
}
