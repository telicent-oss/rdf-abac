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
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.telicent.jena.abac.core.AuthzException;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.QuadFilter;
import io.telicent.jena.abac.core.VocabAuthzDataset;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("java:S2386")
public class Labels {

    private Labels() {
        // No-op.
    }

    public static final Logger LOG = LoggerFactory.getLogger(Labels.class);

    public static QuadFilter securityFilterByLabel(LabelsGetter labels, Label defaultLabel, CxtABAC cxt) {
        return new SecurityFilterByLabel(labels, defaultLabel, cxt);
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
        if (graph != null) {
            labelsStore.addGraph(graph);
        }
        return labelsStore;
    }

    /**
     * Cache/registry of all LabelsStoreRocksDB allocations.
     */
    public static Map<File, LabelsStore> rocks = new ConcurrentHashMap<>();

    /**
     * Factory for a RocksDB-based label store
     * <p>
     * Produces a {@link DictionaryLabelStoreRocksDB}, migrating legacy databases when necessary.
     * </p>
     *
     * @param dbRoot        the root directory of the RocksDB database.
     * @param resource      App configuration inspected for ignored legacy properties; storageFormat supplies the format
     * @param storageFormat the storage format to use within RocksDB
     * @return a labels store which stores its labels in a RocksDB database at {@code dbRoot}
     */
    public static LabelsStore createLabelsStoreRocksDB(final File dbRoot, final Resource resource,
                                                       final StoreFmt storageFormat) {
        return rocks.computeIfAbsent(dbRoot, f -> {
            if (resource != null) {
                for (Property property : new Property[] { VocabAuthzDataset.pLabelsStoreLegacy,
                        VocabAuthzDataset.pLabelsStoreByString, VocabAuthzDataset.pLabelsStoreByHash,
                        VocabAuthzDataset.pLabelsStoreByteBufferSize }) {
                    if (resource.hasProperty(property)) {
                        LOG.warn("Configuration property {} is ignored. RocksDB labels always use the dictionary "
                                 + "store. Existing legacy data at {} will migrate automatically on opening; "
                                 + "back up the database before upgrading because migration cannot be reversed.",
                                 property, dbRoot);
                    }
                }
            }
            try {
                return new DictionaryLabelStoreRocksDB(dbRoot, storageFormat);
            } catch (RocksDBException | IOException e) {
                throw new RuntimeException("Failed to open RocksDB store", e);
            }
        });
    }

    /**
     * A RocksDB-based labels store must be closed Although they are {@link AutoCloseable} sometimes the close needs to
     * be explicit
     *
     * @param labelsStore the store to close
     */
    public static void closeLabelsStoreRocksDB(final LabelsStore labelsStore) {
        try {
            if (labelsStore != null) {
                labelsStore.close();
            }
        } catch (Exception e) {
            LOG.error("Problem closing RocksDB label store {}", e.getMessage(), e);
            throw new AuthzException("Problem closing RocksDB label store", e);
        }
    }

    /**
     * Fine grain control of filter logging. This can be very verbose so sometimes only parts of test development need
     * this.
     */
    public static void setLabelFilterLogging(boolean value) {
        SecurityFilterByLabel.setDebug(value);
    }

    public static boolean getLabelFilterLogging() {
        return SecurityFilterByLabel.getDebug();
    }
}
