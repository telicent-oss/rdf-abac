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

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import io.telicent.jena.abac.labels.StoreFmt;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Concrete triple pattern testing, including patterns (no wildcards).
 */
public abstract class AbstractTestLabelMatchPattern {

    private static final Node ANY_MARKER = Node.ANY;
    private static final Node s = SSE.parseNode(":s");
    private static final Node s1 = SSE.parseNode(":s1");
    private static final Node p = SSE.parseNode(":p");
    private static final Node p1 = SSE.parseNode(":p1");
    private static final Node o = SSE.parseNode(":o");
    private static final Node o1 = SSE.parseNode(":o1");

    protected abstract LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt);

    static Stream<Arguments> provideLabelAndStorageFmt() {
        return Stream.of(Arguments.of(null, null));
    }

    LabelsStore createStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        LabelsStore labels = createLabelsStore(labelMode, storeFmt);
        labels.add(s, p, o, Label.fromText("spo"));
        labels.add(s, p, ANY_MARKER, Label.fromText("sp_"));
        labels.add(s, ANY_MARKER, ANY_MARKER, List.of(Label.fromText("s__"), Label.fromText("x__")));
        labels.add(ANY_MARKER, p, ANY_MARKER, Label.fromText("_p_"));
        labels.add(ANY_MARKER, ANY_MARKER, ANY_MARKER, List.of(Label.fromText("___"), Label.fromText("any=true")));
        return labels;
    }

    static Triple triple(String string) {
        return SSE.parseTriple(string);
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_basic(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try (LabelsStore labels = createLabelsStore(labelMode, storeFmt)) {
            Triple t = triple("(:s1 :p1 :o1)");
            List<Label> x = labels.labelsForTriples(t);
            assertEquals(List.of(), x);
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_spo(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try (LabelsStore ls = createStore(labelMode, storeFmt)) {
            match(s, p, o, ls, Label.fromText("spo"));
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_spx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try (LabelsStore ls = createStore(labelMode, storeFmt)) {
            match(s, p, o1, ls, Label.fromText("sp_"));
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_sxx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try (LabelsStore ls = createStore(labelMode, storeFmt)) {
            match(s, p1, o1, ls, Label.fromText("s__"), Label.fromText("x__"));
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_xpx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try (LabelsStore ls = createStore(labelMode, storeFmt)) {
            match(s1, p, o1, ls, Label.fromText("_p_"));
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void label_match_xxx(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try (LabelsStore ls = createStore(labelMode, storeFmt)) {
            match(s1, p1, o1, ls, Label.fromText("___"), Label.fromText("any=true"));
            match(s1, p1, o1, ls, Label.fromText("any=true"), Label.fromText("___"));
        }
    }

    private void match(Node s, Node p, Node o, LabelsStore labels, Label... expected) {
        Triple triple = Triple.create(s, p, o);
        List<Label> x = labels.labelsForTriples(triple);
        List<Label> e = Arrays.asList(expected);
        ABACTests.assertEqualsUnordered(e, x);
    }
}
