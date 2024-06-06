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

package io.telicent.jena.abac;

import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Concrete triple pattern testing, no patterns (no wildcards).
 */
public abstract class AbstractTestLabelMatch {

    private static final Node ANY_MARKER = Node.ANY;
    private static final Node s = SSE.parseNode(":s");
    private static final Node s1 = SSE.parseNode(":s1");
    private static final Node p = SSE.parseNode(":p");
    private static final Node p1 = SSE.parseNode(":p1");
    private static final Node o = SSE.parseNode(":o");
    private static final Node o1 = SSE.parseNode(":o1");

    private LabelsStore labels;

    protected abstract LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode);

    protected abstract void destroyLabelsStore(LabelsStore labels);

    void createStore(LabelsStoreRocksDB.LabelMode labelMode) {
        labels = createLabelsStore(labelMode);

        labels.add(s, p, o, "spo");
        labels.add(s, p, ANY_MARKER, "sp_");
        labels.add(s, ANY_MARKER, ANY_MARKER, List.of("s__", "x__"));
        labels.add(ANY_MARKER, p, ANY_MARKER, "_p_");
        labels.add(ANY_MARKER, ANY_MARKER, ANY_MARKER, List.of("___", "any=true"));
    }

    @AfterEach void destroyStore() {
        destroyLabelsStore(labels);
    }

    static Triple triple(String string) { return SSE.parseTriple(string); }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void label_match_basic(LabelsStoreRocksDB.LabelMode labelMode) {
        LabelsStore emptyLabelStore = createLabelsStore(labelMode);
        Triple t = triple("(:s1 :p1 :o1)");
        List<String> x = emptyLabelStore.labelsForTriples(t);
        assertEquals(List.of(), x);
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void label_match_spo(LabelsStoreRocksDB.LabelMode labelMode) {
        createStore(labelMode);
        match(s, p, o, "spo");
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void label_match_spx(LabelsStoreRocksDB.LabelMode labelMode) {
        createStore(labelMode);
        match(s, p, o1, "sp_");
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void label_match_sxx(LabelsStoreRocksDB.LabelMode labelMode) {
        createStore(labelMode);
        match(s, p1, o1, "s__", "x__");
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void label_match_xpx(LabelsStoreRocksDB.LabelMode labelMode) {
        createStore(labelMode);
        match(s1, p, o1, "_p_");
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void label_match_xxx(LabelsStoreRocksDB.LabelMode labelMode) {
        createStore(labelMode);
        match(s1, p1, o1, "___", "any=true");
        match(s1, p1, o1, "any=true", "___");
    }

    private void match(Node s, Node p, Node o, String...expected) {
        Triple triple = Triple.create(s, p, o);
        List<String> x = labels.labelsForTriples(triple);
        List<String> e = Arrays.asList(expected);
        ABACTests.assertEqualsUnordered(e, x);
    }
}
