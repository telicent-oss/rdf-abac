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

import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.Stream;

import io.telicent.jena.abac.ABACTests;
import io.telicent.jena.abac.AbstractTestLabelsStore;
import io.telicent.jena.abac.labels.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test storing labels, parameterized for LabelsStoreRocksDB.
 * This test suite has tests for both modes of RocksDB (merge and overwrite).
 * <p>
 * See {@link AbstractTestLabelsStore} for the general contract for a labels store.
 * <p>
 * See {@link AbstractTestLabelMatchRocks} for matching labels more generally.s
 */
@SuppressWarnings("deprecation")
public abstract class AbstractTestLabelsStoreRocksDB {

    protected static final Triple triple1 = parseTriple("(:s :p 123)");
    protected static final Triple triple2 = parseTriple("(:s :p 'xyz')");

    protected abstract LabelsStore createLabelsStore(StoreFmt storeFmt);

    protected abstract LabelsStore createLabelsStore(StoreFmt storeFmt, Graph graph);

    protected void deleteLabelsStore(){
    }

    protected void closeLabelsStore(){
        Labels.closeLabelsStoreRocksDB(store);
        Labels.rocks.clear();
        store = null;
    }

    static Stream<Arguments> provideStorageFormat() {
        return Stream.of(Arguments.of( null));
    }

    protected LabelsStore store;

    @AfterEach
    public void close() {
        deleteLabelsStore();
        closeLabelsStore();
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void labelsStore_1(StoreFmt storeFmt) {
        store = createLabelsStore(storeFmt);
        Label x = store.labelForTriple(triple1);
        assertNull(x);
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void labelsStore_2(StoreFmt storeFmt) {
        store = createLabelsStore(storeFmt);
        store.add(triple1, Label.fromText("triplelabel"));
        Label x = store.labelForTriple(triple1);
        assertEquals(Label.fromText("triplelabel"), x);
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void labelsStore_3(StoreFmt storeFmt) {
        store = createLabelsStore(storeFmt);
        store.add(triple1, Label.fromText("label-1"));
        store.add(triple2, Label.fromText("label-x"));
        store.add(triple1, Label.fromText("label-2"));
        Label x = store.labelForTriple(triple1);
        assertEquals(Label.fromText("label-2"), x);
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void labelsStore_4(StoreFmt storeFmt) {
        store = createLabelsStore(storeFmt);
        store.add(triple1, Label.fromText("label-1"));
        store.add(triple2, Label.fromText("label-2"));
        Label x = store.labelForTriple(triple1);
        assertEquals(Label.fromText("label-1"), x);
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void labels_add_bad_labels_graph( StoreFmt storeFmt) {
        store = createLabelsStore(storeFmt);
        String gs = """
            PREFIX : <http://example>
            PREFIX authz: <http://telicent.io/security#>
            [ authz:pattern 'jibberish' ;  authz:label "allowed" ] .
            """;
        Graph addition = RDFParser.fromString(gs, Lang.TTL).toGraph();

        ABACTests.loggerAtLevel(Labels.LOG, "FATAL", ()-> assertThrows(LabelsException.class, ()-> {
                         store.addGraph(addition);
                         store.labelForTriple(triple1);
        }));
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void labels_add_same_triple_different_label( StoreFmt storeFmt) {
        store = createLabelsStore(storeFmt);
        Label x = store.labelForTriple(triple1);
        assertNull(x, "Labels aready exist");
        store.add(triple1, Label.fromText("label-1"));
        store.add(triple1, Label.fromText("label-2"));
        Label labels = store.labelForTriple(triple1);
        assertEquals(Label.fromText("label-2"), labels);

    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void labels_add_same_triple_same_label( StoreFmt storeFmt) {
        store = createLabelsStore(storeFmt);
        store.add(triple1, Label.fromText("TheLabel"));
        store.add(triple1, Label.fromText("TheLabel"));
        Label labels = store.labelForTriple(triple1);
        assertEquals(Label.fromText("TheLabel"), labels);
    }
}
