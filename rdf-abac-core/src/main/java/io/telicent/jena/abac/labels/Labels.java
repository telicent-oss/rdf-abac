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
import io.telicent.smart.cache.storage.labels.CachingDictionaryLabelsStore;
import io.telicent.smart.cache.storage.labels.DictionaryLabelsStore;
import io.telicent.smart.cache.storage.labels.rocksdb.RocksDbLabelsStore;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.sparql.core.*;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Labels {

    public static final Logger LOG = LoggerFactory.getLogger(Labels.class);

    public static final int DEFAULT_DICTIONARY_CACHE_SIZE = 1000;

    /**
     * How to deal with receiving a label to an item that already has a label.
     */
    static final MultipleLabelPolicy multipleLabelPolicy = MultipleLabelPolicy.REPLACE;

    public static QuadFilter securityFilterByLabel(DatasetGraph dsgBase, LabelsGetter labels, Label defaultLabel, CxtABAC cxt) {
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
        if (graph != null)
            labelsStore.addGraph(graph);
        return labelsStore;
    }

    /**
     * Cache/registry of all LabelsStoreRocksDB allocations.
     */
    public static Map<File, LabelsStoreRocksDB> rocks = new ConcurrentHashMap<>();

    /**
     * Factory for a RocksDB-based label store which stores representations of nodes.
     *
     * @param dbRoot        the root directory of the RocksDB database.
     * @param labelMode     indicates whether to overwrite or merge labels
     * @param resource      RDF Node representing the given apps configuration
     * @param storageFormat the storage format to use within RocksDB
     * @return a labels store which stores its labels in a RocksDB database at {@code dbRoot}
     */
    public static LabelsStore createLabelsStoreRocksDB(
            final File dbRoot,
            final LabelsStoreRocksDB.LabelMode labelMode,
            final Resource resource,
            final StoreFmt storageFormat) throws RocksDBException {
        return rocks.computeIfAbsent(dbRoot, f ->
                new LabelsStoreRocksDB(new RocksDBHelper(), dbRoot, storageFormat, labelMode, resource));
    }

    /**
     * Factory for a RocksDB-based label store which stores representations of nodes and uses a dictionary for compact
     * integer-based label storage.
     *
     * @param dbRoot                the root directory of the RocksDB database.
     * @param labelMode             indicates whether to overwrite or merge labels
     * @param resource              RDF Node representing the given apps configuration
     * @param storageFormat         the storage format to use within RocksDB
     * @param dictionaryLabelsStore the dictionary for mapping labels to integer IDs
     * @return a labels store which stores its labels in a RocksDB database at {@code dbRoot}
     */
    public static LabelsStore createLabelsStoreRocksDB(
            final File dbRoot,
            final LabelsStoreRocksDB.LabelMode labelMode,
            final Resource resource,
            final StoreFmt storageFormat,
            final DictionaryLabelsStore dictionaryLabelsStore) throws RocksDBException {
        return rocks.computeIfAbsent(dbRoot, f ->
                new LabelsStoreRocksDB(new RocksDBHelper(), dbRoot, storageFormat, labelMode, resource, dictionaryLabelsStore));
    }

    /**
     * Create a {@link CachingDictionaryLabelsStore} backed by a {@link RocksDbLabelsStore}.
     *
     * @param dictionaryDir directory for the dictionary's RocksDB instance
     * @param cacheSize     number of entries to cache in each direction (label-to-ID and ID-to-label)
     * @return a caching dictionary labels store
     */
    public static DictionaryLabelsStore createDictionaryLabelsStore(final File dictionaryDir, final int cacheSize) {
        try {
            final RocksDbLabelsStore rocksDictionary = new RocksDbLabelsStore(dictionaryDir);
            return new CachingDictionaryLabelsStore(rocksDictionary, cacheSize);
        } catch (IOException | RocksDBException e) {
            throw new RuntimeException("Failed to create dictionary labels store at " + dictionaryDir, e);
        }
    }

    /**
     * A RocksDB-based labels store must be closed
     * Although they are {@link AutoCloseable} sometimes the close needs to be explicit
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
    public static void setLabelFilterLogging(boolean value) {
        SecurityFilterByLabel.setDebug(value);
    }

    public static boolean getLabelFilterLogging() {
        return SecurityFilterByLabel.getDebug();
    }
}

