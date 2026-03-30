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
import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Labels {

    public static final Logger LOG = LoggerFactory.getLogger(Labels.class);
    //@formatter:off
    public static final String LEGACY_STORE_CONFIGURED =
        """
            Configured to use legacy RocksDB store, consider setting the {} property to false in your RDF configuration
            to use the modern RocksDB store implementation.
        
            This offers reduced storage utilisation and increased performance, existing legacy stores are automatically
            migrated to this format the first time they are opened with the new implementation.
        """;
    //@formatter:on

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
     * This may produce either a {@link LegacyLabelsStoreRocksDB} or a {@link DictionaryLabelStoreRocksDB} depending on
     * the RDF resource supplied from the app configuration.
     * </p>
     *
     * @param dbRoot        the root directory of the RocksDB database.
     * @param resource      RDF Node representing the given apps configuration
     * @param storageFormat the storage format to use within RocksDB
     * @return a labels store which stores its labels in a RocksDB database at {@code dbRoot}
     */
    @SuppressWarnings("deprecation")
    public static LabelsStore createLabelsStoreRocksDB(final File dbRoot, final Resource resource,
                                                       final StoreFmt storageFormat) throws RocksDBException {
        return rocks.computeIfAbsent(dbRoot, f -> {
            // Decide whether to create a legacy or modern store
            // For now, and for backwards compatibility, we treat the new authz:labelsStoreLegacy property as having a
            // default value of true even if not present.  This ensures that pre-existing configurations automatically
            // continue to work as-is without any behavioural changes.
            //
            // This way users who are ready to adopt the modern store can set the property explicitly to false in order
            // to opt in to using the modern store.
            //
            // In some future post 3.0.0 release we'll change the default to false, i.e. automatically opt in users, so
            // they'll eventually need to opt out instead.
            boolean legacyMode = true;
            Statement legacyModeStatement =
                    resource != null ? resource.getProperty(VocabAuthzDataset.pLabelsStoreLegacy) : null;
            RDFNode legacyModeValue = legacyModeStatement != null ? legacyModeStatement.getObject() : null;
            if (legacyModeValue != null && legacyModeValue.isLiteral()) {
                legacyMode = legacyModeValue.asLiteral().getBoolean();
            }
            try {
                if (legacyMode) {
                    // Log a warning suggesting users consider migrating to the new store
                    LOG.warn(LEGACY_STORE_CONFIGURED, VocabAuthzDataset.pLabelsStoreLegacy);
                    return new LegacyLabelsStoreRocksDB(new RocksDBHelper(), dbRoot, storageFormat, resource);
                } else {
                    return new DictionaryLabelStoreRocksDB(dbRoot, storageFormat);
                }
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
     * Run RocksDB-based compaction on a RocksDB-based label store.
     * <p>
     * In normal operation, RocksDB will invoke (background) compaction itself when necessary, this call is most often
     * used to force compaction at the end of a test run to produce predictable database size/performance results for
     * comparison.
     *
     * @param labelsStore the store to compact
     */
    @SuppressWarnings("deprecation")
    public static void compactLabelsStoreRocksDB(final LabelsStore labelsStore) {
        try {
            if (labelsStore instanceof LegacyLabelsStoreRocksDB labelsStoreRocksDB) {
                labelsStoreRocksDB.compact();
            }
        } catch (Exception e) {
            LOG.error("Problem compacting RocksDB label store {}", e.getMessage(), e);
            throw new AuthzException("Problem compacting RocksDB label store", e);
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

