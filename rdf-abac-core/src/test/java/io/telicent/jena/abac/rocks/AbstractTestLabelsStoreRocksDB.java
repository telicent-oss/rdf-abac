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

import static io.telicent.jena.abac.ABACTests.assertEqualsUnordered;
import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.ABACTests;
import io.telicent.jena.abac.AbstractTestLabelsStore;
import io.telicent.jena.abac.labels.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Test storing labels, parameterized for LabelsStoreRocksDB.
 * This test suite has tests for both modes of RocksDB (merge and overwrite).
 * <p>
 * See {@link AbstractTestLabelsStore} for the general contract for a labels store.
 * <p>
 * See {@link AbstractTestLabelMatchRocks} for matching labels more generally.s
 */
public abstract class AbstractTestLabelsStoreRocksDB {

    protected static final Triple triple1 = parseTriple("(:s :p 123)");
    protected static final Triple triple2 = parseTriple("(:s :p 'xyz')");

    protected abstract LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode);

    protected abstract LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, Graph graph);

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void labelsStore_1(LabelsStoreRocksDB.LabelMode labelMode) {
        LabelsStore store = createLabelsStore(labelMode);
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of(), x);
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void labelsStore_2(LabelsStoreRocksDB.LabelMode labelMode) {
        LabelsStore store = createLabelsStore(labelMode);
        store.add(triple1, "triplelabel");
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of("triplelabel"), x);
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void labelsStore_3(LabelsStoreRocksDB.LabelMode labelMode) {
        LabelsStore store = createLabelsStore(labelMode);
        store.add(triple1, "label-1");
        store.add(triple2, "label-x");
        store.add(triple1, "label-2");
        List<String> x = store.labelsForTriples(triple1);
        if (this instanceof BaseTestLabelsStoreRocksDB && labelMode == LabelsStoreRocksDB.LabelMode.Merge) {
            assertEqualsUnordered(List.of("label-1", "label-2"), x);
        } else {
            assertEquals(List.of("label-2"), x);
        }
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void labelsStore_4(LabelsStoreRocksDB.LabelMode labelMode) {
        LabelsStore store = createLabelsStore(labelMode);
        store.add(triple1, "label-1");
        store.add(triple2, "label-2");
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of("label-1"), x);
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void labels_add_bad_label(LabelsStoreRocksDB.LabelMode labelMode) {
        // Label is a parse error.
        String logLevel = "FATAL";
        LabelsStore store = createLabelsStore(labelMode);
        ABACTests.loggerAtLevel(ABAC.AttrLOG, logLevel, ()->{
            assertThrows(LabelsException.class, ()->store.add(triple1, "not .. good (LabelsStoreRocksDB)"));
        });
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void labels_add_bad_labels_graph(LabelsStoreRocksDB.LabelMode labelMode) {
        LabelsStore store = createLabelsStore(labelMode);
        String gs = """
            PREFIX : <http://example>
            PREFIX authz: <http://telicent.io/security#>
            [ authz:pattern 'jibberish' ;  authz:label "allowed" ] .
            """;
        Graph addition = RDFParser.fromString(gs, Lang.TTL).toGraph();

        ABACTests.loggerAtLevel(Labels.LOG, "FATAL", ()->{
            assertThrows(LabelsException.class, ()-> {
                             store.addGraph(addition);
                             store.labelsForTriples(triple1);
            });
        });
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void labels_add_same_triple_different_label(LabelsStoreRocksDB.LabelMode labelMode) {
        LabelsStore store = createLabelsStore(labelMode);
        List<String> x = store.labelsForTriples(triple1);
        assertTrue(x.isEmpty(), "Labels aready exist");
        store.add(triple1, "label-1");
        store.add(triple1, "label-2");
        List<String> labels = store.labelsForTriples(triple1);

        if (this instanceof BaseTestLabelsStoreRocksDB && labelMode == LabelsStoreRocksDB.LabelMode.Merge) {
            assertEqualsUnordered(List.of("label-1", "label-2"), labels);
        } else {
            assertEquals(List.of("label-2"), labels);
        }
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void labels_add_same_triple_same_label(LabelsStoreRocksDB.LabelMode labelMode) {
        LabelsStore store = createLabelsStore(labelMode);
        List<String> x = store.labelsForTriples(triple1);
        store.add(triple1, "TheLabel");
        store.add(triple1, "TheLabel");
        List<String> labels = store.labelsForTriples(triple1);
        assertEquals(List.of("TheLabel"), labels);
    }

    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void labels_add_triple_duplicate_label_in_list(LabelsStoreRocksDB.LabelMode labelMode) {
        LabelsStore store = createLabelsStore(labelMode);
        List<String> x = store.labelsForTriples(triple1);
        var z = List.of("TheLabel", "TheLabel");
        assertThrows(LabelsException.class, ()->store.add(triple1, z));
    }
}
