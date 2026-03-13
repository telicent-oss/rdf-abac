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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import io.telicent.jena.abac.SysABAC;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.core.TransactionalNull;
import org.apache.jena.system.Txn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory labels store, concrete triple to label map, no patterns matched.
 */
public class LabelsStoreMem implements LabelsStore {

    private static final Logger LOG = LoggerFactory.getLogger(LabelsStoreMem.class);

    // Quad to label indexing.
    // This is not protected by the transactional.
    // It is thread-safe but may be read inconsistently.
    private final Map<Quad, Label> quadLabels = new ConcurrentHashMap<>();

    // Update accumulator used to collect updates which are then flushed to tripleLabels on commit.
    // This allows for a data load operation to abort. The accumulator is flushed
    // after data has been loaded into the triple store, so it is syntactically valid,
    // and before the data is committed and becomes visible.
    //
    // We prefer better availability (writes do not block readers) over consistency.
    // Consistency is minimised for slow updates by use of the accumulator.

    private final Map<Quad, Label> accQuadLabels = new ConcurrentHashMap<>();

    // Future: Consider binding LabelsStore to the DatasetGraphABAC transactional so
    // that operations on the labels side are also protected.
    // While all operation go through a DatasetGraphABAC, the dataset is MR+SW (reads can overlap writes).
    // We choose availability (see asGraph()) over consistency.

    // Used to give consistent update flushing accumulated triples.
    private final Transactional transactional;

    @Override
    public void close() throws Exception {
    }

    /**
     * TransactionalNull tracks the transaction state so these calls know if they are part of a write transaction.
     */
    private class TransactionalHook extends TransactionalNull {
        @Override
        public void commit() {
            // transactionMode() reflects the current thread transaction state.
            if (super.transactionMode() == ReadWrite.WRITE) {
                // Flush the triple label accumulator to the main store.
                flushAccumulator();
            }
            super.commit();
        }

        @Override
        public void abort() {
            if (super.transactionMode() == ReadWrite.WRITE) {
                clearAccumulator();
            }
            super.abort();
        }
    }

    /**
     * A fresh, empty in-memory {@link LabelsStore}
     */
    public static LabelsStore create() {
        return new LabelsStoreMem();
    }

    /**
     * Creates a new empty in-memory labels store
     */
    private LabelsStoreMem() {
        // Called from DatasetGraphABAC in its transaction lifecycle.
        this.transactional = new TransactionalHook();
    }

    @Override
    public Transactional getTransactional() {
        return transactional;
    }

    // ---- Read operations ----

    @Override
    public Label labelForQuad(Quad quad) {
        if (!quad.isConcrete()) {
            LOG.error("Asked for labels for a quad with wildcards: {}", NodeFmtLib.displayStr(quad));
            return null;
        }

        try {
            readOperation();
        } catch (AuthzTriplePatternException ex) {
            LOG.error("Failed to update index: {}", ex.getMessage());
            return SysABAC.denyLabel;
        }

        return this.quadLabels.get(quad);
    }

    /**
     * Signal a read operation.
     */
    private void readOperation() {
        // Needed when the store is standalone (e.g. tests!) and there is no transaction lifecycle in use.
        if (!transactional.isInTransaction()) {
            flushAccumulator();
        }
    }

    /**
     * Signal a write operation.
     */
    private void writeOperation() {
    }

    @Override
    public void forEach(BiConsumer<Quad, Label> action) {
        readOperation();
        quadLabels.forEach(action);
    }

    @Override
    public Graph asGraph() {
        readOperation();
        Graph gResult = L.newLabelGraph();
        // Thread-safe, but not guaranteed to be consistent.
        L.labelsToGraph(this, gResult);
        return gResult;
    }

    /**
     * The properties of the default label store include the size of the labels map.
     *
     * @return the properties of the label store
     */
    @Override
    public Map<String, String> getProperties() {
        final var properties = new HashMap<String, String>();
        properties.put("size", "" + quadLabels.size());
        return properties;
    }

    @Override
    public boolean isEmpty() {
        readOperation();
        return quadLabels.isEmpty();
    }

    private void flushAccumulator() {
        if (!accQuadLabels.isEmpty()) {
            // Ensure only one thread is emptying the accumulator.
            // While ConcurrentHashMaps are safe, the putAll and clear need to be one step.
            synchronized (this) {
                // This is the only write to the main triples->labels map.
                // accTripleLabels is not protected but either:
                //   Transaction in use and only the write transaction will update accTripleLabels.
                //   Freestanding usage in tests when all work is single threaded.
                quadLabels.putAll(accQuadLabels);
                clearAccumulator();
            }
        }
    }

    private void clearAccumulator() {
        accQuadLabels.clear();
    }

    // ---- Update operations  ----

    @Override
    public void addGraph(Graph labels) {
        writeOperation();
        if (transactional.isInTransaction()) {
            add$(labels);
            return;
        }
        Txn.executeWrite(transactional, () -> add$(labels));
    }

    private void add$(Graph labelsGraph) {
        // Check the small incoming graph, this throws an error if the graph is malformed
        L.checkShape(labelsGraph);
        // Concrete triples only
        L.loadStoreFromGraph(this, labelsGraph);
    }

    @Override
    public void add(Quad quad, Label label) {
        writeOperation();
        add$(quad, label);
    }

    /**
     * Add a triple pattern but do not rebuild index.
     */
    private void add$(Quad quad, Label label) {
        if (!quad.isConcrete()) {
            LOG.error("Tried to add label for a quad with wildcards: {}", NodeFmtLib.displayStr(quad));
        }
        accQuadLabels.put(quad, label);
    }

    @Override
    public void remove(Quad quad) {
        this.quadLabels.remove(quad);
        this.accQuadLabels.remove(quad);
    }

    @Override
    public String toString() {
        return String.format("%s[%d]", this.getClass().getSimpleName(), quadLabels.size());
    }

}
