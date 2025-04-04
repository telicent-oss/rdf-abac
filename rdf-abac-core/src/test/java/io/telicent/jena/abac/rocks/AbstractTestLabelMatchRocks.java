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

package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.ABACTests;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import io.telicent.jena.abac.labels.StoreFmt;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Concrete triple pattern testing (no wildcards) with all Rocks modes
 */
abstract class AbstractTestLabelMatchRocks {

    private static final Node ANY_MARKER = Node.ANY;
    private static final Node s = SSE.parseNode(":s");
    private static final Node s1 = SSE.parseNode(":s1");
    private static final Node p = SSE.parseNode(":p");
    private static final Node p1 = SSE.parseNode(":p1");
    private static final Node o = SSE.parseNode(":o");
    private static final Node o1 = SSE.parseNode(":o1");

    private File dbDirectory;

    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        try {
            dbDirectory = Files.createTempDirectory("tmp" + storeFmt.getClass()).toFile();
            return Labels.createLabelsStoreRocksDB(dbDirectory, labelMode, null, storeFmt);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    static Stream<Arguments> provideLabelAndStorageFmt() {
        return Stream.of(Arguments.of(null, null));
    }

    LabelsStore createStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        LabelsStore labels = createLabelsStore(labelMode, storeFmt);
        labels.add(s, p, o, "spo");
        labels.add(s, p, ANY_MARKER, "sp_");
        labels.add(s, ANY_MARKER, ANY_MARKER, List.of("s__", "x__"));
        labels.add(ANY_MARKER, p, ANY_MARKER, "_p_");
        labels.add(ANY_MARKER, ANY_MARKER, ANY_MARKER, List.of("___", "any=true"));
        return labels;
    }

    @AfterEach void destroyStore() {
        dbDirectory.delete();
        dbDirectory = null;
        Labels.rocks.clear();
    }

    static Triple triple(String string) { return SSE.parseTriple(string); }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_basic(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try(LabelsStore emptyLabelStore = createLabelsStore(labelMode, storeFmt)) {
            Triple t = triple("(:s1 :p1 :o1)");
            List<String> x = emptyLabelStore.labelsForTriples(t);
            assertEquals(List.of(), x);
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_spo(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try(LabelsStore ls = createStore(labelMode, storeFmt)) {
            match(s, p, o, ls, "spo");
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_spx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try(LabelsStore ls = createStore(labelMode, storeFmt)) {
            match(s, p, o1, ls, "sp_");
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_sxx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try(LabelsStore ls = createStore(labelMode, storeFmt)) {
            match(s, p1, o1, ls, "s__", "x__");
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_xpx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try(LabelsStore ls = createStore(labelMode, storeFmt)) {
            match(s1, p, o1, ls, "_p_");
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_xxx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try(LabelsStore ls = createStore(labelMode, storeFmt)){
            match(s1, p1, o1, ls, "___", "any=true");
            match(s1, p1, o1, ls, "any=true", "___");
        }
    }

    private void match(Node s, Node p, Node o, LabelsStore labels, String...expected) {
        Triple triple = Triple.create(s, p, o);
        List<String> x = labels.labelsForTriples(triple);
        List<String> e = Arrays.asList(expected);
        ABACTests.assertEqualsUnordered(e, x);
    }
}
