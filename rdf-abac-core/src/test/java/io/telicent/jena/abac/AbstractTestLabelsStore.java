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

import static io.telicent.jena.abac.ABACTests.assertEqualsUnordered;
import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.telicent.jena.abac.labels.L;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsException;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.atlas.lib.ListUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.Test;

/**
 * General label store tests; no triple patterns for labelling.
 */
public abstract class AbstractTestLabelsStore {

    protected static final Triple triple1 = parseTriple("(:s :p 123)");
    protected static final Triple triple2 = parseTriple("(:s :p 'xyz')");

    /** Empty {@link LabelsStore} */
    protected abstract LabelsStore createLabelsStore();

    /**
     * A {@link LabelsStore}, initialized with graph recording labels.
     * The default is to create an empty {@link LabelsStore}
     * and call {@link  L#loadStoreFromGraph}.
     */
    protected LabelsStore createLabelsStore(Graph labelsGraph) {
        LabelsStore labelsStore = createLabelsStore();
        L.loadStoreFromGraph(labelsStore, labelsGraph);
        return labelsStore;
    }

    // ----

    private static String labelsGraphBadPattern = """
        PREFIX foo: <http://example/>
        PREFIX authz: <http://telicent.io/security#>
        ## No bar:
        [ authz:pattern 'bar:s bar:p1 123' ;  authz:label "allowed" ] .
    """;
    private static Graph BAD_PATTERN = RDFParser.fromString(labelsGraphBadPattern, Lang.TTL).toGraph();

    @Test public void labelsStore_noLabel() {
        LabelsStore store = createLabelsStore();
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of(), x);
    }

    @Test public void labelsStore_addLabel() {
        LabelsStore store = createLabelsStore();
        store.add(triple1, "triplelabel");
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of("triplelabel"), x);
    }

    @Test public void labelsStore_addLabels_no_interference() {
        LabelsStore store = createLabelsStore();
        store.add(triple1, "label1");
        store.add(triple2, "label2");
        List<String> x = store.labelsForTriples(triple1);
        assertEquals(List.of("label1"), x);
    }

    @Test public void labelsStore_addLabels_addDifferentLabel() {
        LabelsStore store = createLabelsStore();
        store.add(triple1, "label1");
        store.add(triple2, "labelx");
        store.add(triple1, "label2");
        List<String> x1 = store.labelsForTriples(triple1);
        assertEquals(1, x1.size());
        assertEquals(x1, List.of("label2"));
        List<String> x2 = store.labelsForTriples(triple2);
        assertEquals(x2, List.of("labelx"));
    }

    @Test public void labelsStore_addLabel_is_empty() {
        LabelsStore store = createLabelsStore();
        assertTrue(store.isEmpty(), "Store is not empty on creation");
        store.add(triple1, "label1");
        assertFalse(store.isEmpty(), "Store is empty after adding a label");
    }

    @Test public void labels_bad_labels_graph() {
        assertThrows(LabelsException.class,
                     ()-> ABACTests.loggerAtLevel(Labels.LOG, "FATAL",
                                                  ()->createLabelsStore(BAD_PATTERN))  // warning and error
                );
    }

    @Test public void labels_add_bad_label() {
        // Label is a parse error.
        String logLevel = "FATAL";
        LabelsStore store = createLabelsStore();
        ABACTests.loggerAtLevel(ABAC.AttrLOG, logLevel, ()->{
            assertThrows(LabelsException.class, ()->store.add(triple1, "not .. good"));
        });
    }

    @Test public void labels_add_bad_labels_graph() {
        LabelsStore store = createLabelsStore();
        String gs = """
                PREFIX : <http://example>
                PREFIX authz: <http://telicent.io/security#>
                [ authz:pattern 'jibberish' ;  authz:label "allowed" ] .
                """;
        Graph addition = RDFParser.fromString(gs, Lang.TTL).toGraph();

        ABACTests.loggerAtLevel(Labels.LOG, "FATAL", ()->{
            assertThrows(LabelsException.class,
                     ()-> {
                             store.addGraph(addition);
                             store.labelsForTriples(triple1);
                     });
        });
    }

    @Test public void labels_add_same_triple_different_label() {
        LabelsStore store = createLabelsStore();
        List<String> x = store.labelsForTriples(triple1);
        store.add(triple1, "label-1");
        store.add(triple1, "label-2");

        List<String> labels = store.labelsForTriples(triple1);
        List<String> expected = List.of("label-2");
        // Order can not be assumed.
        assertTrue(ListUtils.equalsUnordered(expected, labels), "Expected: "+expected+"  Got: "+labels);
    }

    @Test public void labels_add_triple_multiple_label() {
        LabelsStore store = createLabelsStore();
        List<String> x = store.labelsForTriples(triple1);
        store.add(triple1, List.of("label-1", "label-2"));
        List<String> labels = store.labelsForTriples(triple1);
        List<String> expected = List.of("label-1", "label-2");
        // Order is not preserved
        assertEqualsUnordered(expected, labels);
    }

    @Test public void labels_add_same_triple_same_label() {
        LabelsStore store = createLabelsStore();
        List<String> x = store.labelsForTriples(triple1);
        store.add(triple1, "TheLabel");
        store.add(triple1, "TheLabel");
        List<String> labels = store.labelsForTriples(triple1);
        assertEquals(List.of("TheLabel"), labels);
    }

    @Test public void labels_add_triple_duplicate_label_in_list() {
        LabelsStore store = createLabelsStore();
        List<String> x = List.of("TheLabel", "TheLabel");
        assertThrows(LabelsException.class, ()->store.add(triple1, x));
    }
}
