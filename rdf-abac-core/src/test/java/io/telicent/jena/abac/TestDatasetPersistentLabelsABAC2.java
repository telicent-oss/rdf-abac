/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.telicent.jena.abac;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.VocabAuthzDataset;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.TxnType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.system.G;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * See also {@link TestDatasetPersistentLabelsABAC}.
 * <p>
 * The test here is stable on it owns, but not in a larger suite.
 * It is as if RocksDB does not completely clear up fast enough.
 */
public class TestDatasetPersistentLabelsABAC2 {

    static {
        // Initialize so that these tests can be run standalone.
        JenaSystem.init();
    }

    private static String logAuthzLevel = null;

    @BeforeAll public static void beforeAll() {
        logAuthzLevel = LogCtl.getLevel(SysABAC.SYSTEM_LOG);
        LogCtl.setLevel(SysABAC.SYSTEM_LOG, "Error");
        // ... otherwise "LabelsStore[LabelsStoreRocksDB] provided for in-memory database"
        // For convenience and focus, tests work with an in-memory database and empty
        // label store each time.
    }

    @AfterAll public static void afterAll() {
        if ( logAuthzLevel != null )
            LogCtl.setLevel(SysABAC.SYSTEM_LOG, logAuthzLevel);
    }

    private DatasetGraphABAC create() {
        return createDynamic();
    }

    private static int counter = 0;
    private DatasetGraphABAC createDynamic() {
        String dbPath = "target/LABELS"+(counter++);
        FileOps.clearAll(dbPath);
        FileOps.delete(dbPath);
        // The Dataset is in-memory, the label store is on-disk and empty.
        String config = "src/test/files/dataset/abac-dsg-rocks.ttl";

        Model spec = RDFParser.source(config).toModel();

        // Unique paths
        Graph g = spec.getGraph();
        Triple t = G.find(g, null, VocabAuthzDataset.pLabelsStorePath.asNode(), null).next();
        Triple t2 = Triple.create(t.getSubject(), t.getPredicate(), NodeFactory.createLiteralString(dbPath));
        g.delete(t);
        g.add(t2);

        Dataset ds = (Dataset)AssemblerUtils.build(spec, VocabAuthzDataset.tDatasetAuthz);
        DatasetGraph dsg = ds.asDatasetGraph();
        DatasetGraphABAC dsgz = (DatasetGraphABAC)dsg;
        return dsgz;
    }

    @Test
    public void dsgz_txn_promote() throws Exception {
        final DatasetGraphABAC dsgz = create();
        Triple t = SSE.parseTriple("(:s :p :o)");
        Quad q = Quad.create(Quad.defaultGraphIRI, t);
        try (final LabelsStore labelsStore = dsgz.labelsStore()) {

            dsgz.exec(TxnType.READ_PROMOTE, () -> {
                assertTrue(dsgz.getDefaultGraph().isEmpty());
                assertTrue(labelsStore.isEmpty());

                dsgz.executeWrite(() -> {
                    dsgz.add(q);
                    labelsStore.add(t, "abcdef");
                });
            });
            dsgz.exec(TxnType.READ, () -> {
                assertFalse(dsgz.getDefaultGraph().isEmpty());
                assertFalse(labelsStore.isEmpty());
            });
        }
    }
}
