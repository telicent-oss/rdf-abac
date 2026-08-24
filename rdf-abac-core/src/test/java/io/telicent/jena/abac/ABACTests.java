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

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.Attributes;
import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.HierarchyGetter;
import io.telicent.jena.abac.core.Track;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.lib.ListUtils;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.GraphUtil;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.exec.QueryExec;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.util.IsoMatcher;
import org.apache.jena.system.G;
import org.slf4j.Logger;

@SuppressWarnings("java:S2187")
public class ABACTests {

    /**
     * Test runner for the all-in-one test description files.
     * See {@link BuildAIO} for the TriG file format.
     */
    public static void runTest(String filename, String user, int count, LabelsStore testSubject) {
        // == Setup.
        DatasetGraph aio = RDFDataMgr.loadDatasetGraph(filename);
        DatasetGraphABAC dsgz = setupByTriG(aio, testSubject);
        Graph expected = aio.getGraph(VocabAuthzTest.graphForTestResult);

        // == Request
        HierarchyGetter function = a -> dsgz.attributesStore().getHierarchy(a);
        DatasetGraph dsgr = ABAC.requestDataset(dsgz, dsgz.attributesForUser().apply(user), function);

        String queryString = "CONSTRUCT WHERE { ?s ?p ?o }";
        // === Result
        Graph actual = QueryExec.dataset(dsgr).query(queryString).construct();

        boolean b = IsoMatcher.isomorphic(expected, actual);
        if ( !b ) {
            System.out.println("** Test: "+filename);
            RDFDataMgr.write(System.out, aio, Lang.TRIG);
            System.out.println("-- Expected");
            RDFDataMgr.write(System.out, expected, Lang.TTL);
            System.out.println("-- Actual");
            RDFDataMgr.write(System.out, actual, Lang.TTL);
            System.out.println("----");
        }
        int x = actual.size();
        assertEquals(count, x);
        assertTrue(b, "Not isomorphic");
    }

    private static DatasetGraphABAC setupByTriG(DatasetGraph aio, LabelsStore testSubject) {
        DatasetGraph data = DatasetGraphFactory.createTxnMem();
        GraphUtil.addInto(data.getDefaultGraph(), aio.getDefaultGraph());
        return buildFromGraphs(data, testSubject,
                               copy(aio.getGraph(VocabAuthzTest.graphForLabels)),
                               copy(aio.getGraph(VocabAuthzTest.graphForAttributes)));
    }

    private static Graph copy(Graph graph) {
        Graph safe = GraphFactory.createDefaultGraph();
        graph.getTransactionHandler().execute(() -> GraphUtil.addInto(safe, graph));
        return safe;
    }

    private static DatasetGraphABAC buildFromGraphs(DatasetGraph data, LabelsStore testSubject, Graph labels,
                                                    Graph attributesStoreGraph) {
        LabelsStore labelsStore = Objects.requireNonNull(testSubject, "Argument testSubject");
        Label datasetDefaultLabel = null;

        if ( labels != null && ! labels.isEmpty() ) {
            labelsStore.addGraph(labels);
            Node x = G.getZeroOrOneSP(labels, null, VocabAuthzTest.pDatasetDefaultLabel);
            if ( x != null ) {
                if ( ! Util.isSimpleString(x) )
                    Log.error(ABACTests.class, "Ignored - not valid for authz:datasetDefaultLabel: " + x);
                else
                    datasetDefaultLabel = Label.fromText(x.getLiteralLexicalForm());
            }
        }

        AttributesStore attributesStore = Attributes.buildStore(attributesStoreGraph);
        return ABAC.authzDataset(data, null, labelsStore, datasetDefaultLabel, attributesStore);
    }

    /** Execute, possibly with the fine-grained ABAC filtering logging turned on. */
    public static void debugABAC(boolean debug, Runnable action) {
        if ( ! debug ) {
            action.run();
            return;
        }
        CxtABAC.systemTrace(Track.DEBUG);
        try {
            action.run();
        } finally {
            CxtABAC.systemTrace(Track.NONE);
        }
    }

    /** Execute, possibly with the fine-grained ABAC filtering logging turned on. */
    public static <X> X calcDebugABAC(boolean debug, Supplier<X> action) {
        if ( ! debug ) {
            return action.get();
        }
        CxtABAC.systemTrace(Track.DEBUG);
        try {
            return action.get();
        } finally {
            CxtABAC.systemTrace(Track.NONE);
        }
    }


    public static void printFile(String filename) {
        System.out.println("-- File: "+filename);
        String s = IO.readWholeFileAsUTF8(filename);
        System.out.println(s);
    }

    public static void loggerAtLevel(Class<?> logger, String runLevel, Runnable action) {
        loggerAtLevel(logger.getName(), runLevel, action);
    }

    /**
     * Run an action with some loggers set to a temporary log level.
     */
    public static void loggerAtLevel(String logger, String runLevel, Runnable action) {
        // Risk of confusion of logger and level.
        Objects.requireNonNull(logger);
        Objects.requireNonNull(runLevel);
        String oldLevel = LogCtl.getLevel(logger);
        LogCtl.setLevel(logger, runLevel);
        try {
            action.run();
        } finally {
            LogCtl.setLevel(logger, oldLevel);
        }
    }

    /**
     * Run an action with some loggers set to a temporary log level.
     */
    public static void loggerAtLevel(Logger logger, String runLevel, Runnable action) {
        Objects.requireNonNull(logger);
        Objects.requireNonNull(runLevel);
        String oldLevel = LogCtl.getLevel(logger);
        LogCtl.setLevel(logger, runLevel);
        try {
            action.run();
        } finally {
            LogCtl.setLevel(logger, oldLevel);
        }
    }


    /**
     * Run an action with some loggers set to a temporary log level.
     */
    public static void silent(String runLevel, String[] loggers, Runnable action) {
        // Not String...loggers - action maybe be inline code.
        if ( loggers.length == 0 )
            System.err.println("Warning: Empty array of loggers passed to silent()");
        Map<String, String> levels = new HashMap<>();
        for ( String logger : loggers ) {
            levels.put(logger, LogCtl.getLevel(logger));
            LogCtl.setLevel(logger, runLevel);
        }
        try {
            action.run();
        } finally {
            levels.forEach(LogCtl::setLevel);
        }
    }

    public static <X> void assertEqualsUnordered(List<X> expected, List<X> actual) {
        boolean b = ListUtils.equalsUnordered(actual, expected);
        if ( !b ) {
            String msg = "Expected: "+expected+", Got: "+actual;
            fail(msg);
        }
    }
}
