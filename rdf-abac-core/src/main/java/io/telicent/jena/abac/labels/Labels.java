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

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.telicent.jena.abac.core.AuthzException;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.QuadFilter;
import org.apache.jena.graph.Graph;
import org.apache.jena.sparql.core.*;
import org.apache.jena.tdb2.store.nodetable.NodeTable;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Labels {

    public static final Logger LOG = LoggerFactory.getLogger(Labels.class);

    /** How to deal with receiving a label to an item that already has a label. */
    static final MultipleLabelPolicy multipleLabelPolicy = MultipleLabelPolicy.REPLACE;

    public static QuadFilter securityFilterByLabel(DatasetGraph dsgBase, LabelsGetter labels, String defaultLabel, CxtABAC cxt) {
        return new SecurityFilterByLabel(dsgBase, labels, defaultLabel, cxt);
    }

    private static final LabelsStore noLabelsStore = new LabelsStoreZero();

    public static LabelsStore emptyStore() {
        return noLabelsStore;
    }

    /**
     * Standalone in-memory label store
     */
    public static LabelsStore createLabelsStoreMem() {
        return LabelsStoreMem.create();
    }

    /**
     * Create a label store; initialize with the labels described in the argument graph.
     */
    public static LabelsStore createLabelsStoreMem(Graph graph) {
        LabelsStore labelsStore = createLabelsStoreMem();
        if ( graph != null )
            labelsStore.addGraph(graph);
        return labelsStore;
    }

    /** Cache/registry of all LabelsStoreRocksDB allocations. */
    public static Map<File, LabelsStoreRocksDB> rocks = new ConcurrentHashMap<>();

    /**
     * Factory for a RocksDB-based label store which stores string representations of nodes.
     *
     * @param dbRoot the root directory of the RocksDB database.
     * @return a labels store which stores its labels in a RocksDB database at {@code dbRoot}
     * @throws RocksDBException if something goes wrong during database creation
     */
    public static LabelsStore createLabelsStoreRocksDBByString(
                final File dbRoot, final LabelsStoreRocksDB.LabelMode labelMode) throws RocksDBException {
        LabelsStoreRocksDB rocksLabelsStore = rocks.computeIfAbsent(dbRoot, f->
                new LabelsStoreRocksDB(dbRoot, new StoreFmtByString(), labelMode) );
        return rocksLabelsStore;
    }

    /**
     * Factory for a RocksDB-based label store which stores id-based representations of nodes.
     * It requires a node table which maps from nodes to ids to accomplish this.
     *
     * @param dbRoot the root directory of the RocksDB database.
     * @param storeNodeTable the node/id table used to map from nodes on the API to ids in storage.
     * @return a labels store which stores its labels in a RocksDB database at {@code dbRoot}
     * @throws RocksDBException if something goes wrong during database creation
     */
    public static LabelsStore createLabelsStoreRocksDBById(
                final File dbRoot, final NodeTable storeNodeTable,
                final LabelsStoreRocksDB.LabelMode labelMode) throws RocksDBException {
        LabelsStoreRocksDB rocksLabelsStore = rocks.computeIfAbsent(dbRoot, f->
                new LabelsStoreRocksDB(dbRoot, new StoreFmtById(storeNodeTable), labelMode) );
        return rocksLabelsStore;
    }

    /**
     * A RocksDB-based labels store must be closed
     * Although they are {@link AutoCloseable} sometimes the close needs to be explicit
     *
     * @param labelsStore the store to close
     */
    public static void closeLabelsStoreRocksDB(final LabelsStore labelsStore) {
        try {
            if (labelsStore instanceof LabelsStoreRocksDB labelsStoreRocksDB) {
                labelsStoreRocksDB.close();
            }
        } catch (Exception e) {
            LOG.error("Problem closing RocksDB label store {}", e.getMessage(), e);
            throw new AuthzException("Problem closing RocksDB label store", e);
        }
    }

    /**
     * Run RocksDB-based compaction on a RocksDB-based label store.
     * <p>
     * In normal operation, RocksDB will invoke (background) compaction itself when necessary,
     * this call is most often used to force compaction at the end of a test run to produce
     * predictable database size/performance results for comparison.
     *
     * @param labelsStore the store to compact
     */
    public static void compactLabelsStoreRocksDB(final LabelsStore labelsStore) {
        try {
            if (labelsStore instanceof LabelsStoreRocksDB labelsStoreRocksDB) {
                labelsStoreRocksDB.compact();
            }
        } catch (Exception e) {
            LOG.error("Problem compacting RocksDB label store {}", e.getMessage(), e);
            throw new AuthzException("Problem compacting RocksDB label store", e);
        }
    }

    /**
     * Fine grain control of filter logging.
     * This can be very verbose so sometimes only parts of test development need this.
     */
    public static void setLabelFilterLogging(boolean value) { SecurityFilterByLabel.setDebug(value); }

    public static boolean getLabelFilterLogging() {return  SecurityFilterByLabel.getDebug(); }
}

