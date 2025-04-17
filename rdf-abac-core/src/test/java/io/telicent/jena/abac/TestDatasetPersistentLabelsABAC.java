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

import static org.apache.jena.atlas.lib.ThreadLib.syncCallThread;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.VocabAuthzDataset;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.Dataset;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.sys.JenaSystem;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestDatasetPersistentLabelsABAC {

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
        FileOps.clearAll("target/LABELS");
        FileOps.delete("target/LABELS");
        // The Dataset is in-memory, the label store is on-disk and empty.
        String config = "src/test/files/dataset/abac-dsg-rocks.ttl";
        Dataset ds = (Dataset)AssemblerUtils.build(config, VocabAuthzDataset.tDatasetAuthz);
        DatasetGraph dsg = ds.asDatasetGraph();
        DatasetGraphABAC dsgz = (DatasetGraphABAC)dsg;
        return dsgz;
    }

    @Test
    public void dsgz_txn_read_write() {
        DatasetGraphABAC dsgz = create();

        Triple t = SSE.parseTriple("(:s :p :o)");
        Quad q = Quad.create(Quad.defaultGraphIRI, t);

        dsgz.executeWrite(()->{
            LabelsStore labelsStore = dsgz.labelsStore();
            dsgz.add(q);
            labelsStore.add(t, Label.fromText("simple"));
            List<Label> labels = dsgz.labelsStore().labelsForTriples(t);
            // May be empty!
            // The RocksDB back store batches writes.
            // The WriteBatch is not been added until the end of the transaction.
            //assertTrue(labels.isEmpty(), "Expected label for triple after write transaction");
        });
        dsgz.executeRead(()->{
            List<Label> labels = dsgz.labelsStore().labelsForTriples(t);
            assertTrue(!labels.isEmpty(), "Expected label for triple after write transaction");
        });
        List<Label> labels = dsgz.labelsStore().labelsForTriples(t);
        assertTrue(!labels.isEmpty(), "Expected label for triple after write transaction");
    }

    @Test
    public void dsgz_txn_write_inner_read() {
        DatasetGraphABAC dsgz = create();
        Triple t = SSE.parseTriple("(:s :p :o)");
        LabelsStore labelsStore = dsgz.labelsStore();
        dsgz.executeWrite(()->{
            dsgz.getDefaultGraph().add(t);
            labelsStore.add(t, Label.fromText("foo"));
            List<Label> labels0 = labelsStore.labelsForTriples(t);

            // On another thread, look into the labelsStore as a READ before
            // the commit WRITE on this thread.
            Boolean result = syncCallThread(()->{
                return Txn.calculateRead(dsgz, ()->{
                    try {
                        LabelsStore labelsStoreInner = dsgz.labelsStore();
                        List<Label> labels = labelsStoreInner.labelsForTriples(t);
                        boolean rc = labels.isEmpty();
                        return rc;
                    } catch (Throwable th) {
                        return null;
                    }
                });
            });
            assertNotNull(result, "Other thread had an error");
            assertFalse(result, "Expected false from other thread");
        });

        List<Label> labels = labelsStore.labelsForTriples(t);
        assertTrue(!labels.isEmpty(), "Expected label for triple after write transaction");
    }

    @Test
    public void dsgz_txn_read_overlapping_write() {
        final DatasetGraphABAC dsgz = create();
        Triple t = SSE.parseTriple("(:s :p :o)");
        Quad q = Quad.create(Quad.defaultGraphIRI, t);
        final LabelsStore labelsStore = dsgz.labelsStore();

        dsgz.executeRead(()->{
            List<Label> labels1 = labelsStore.labelsForTriples(t);

            Boolean result = syncCallThread(()->{
                try {
                    dsgz.executeWrite(()->{
                        dsgz.add(q);
                        dsgz.add(q);
                        labelsStore.add(t, Label.fromText("abcdef"));
                    });
                    return true;
                } catch (Throwable th) {
                    return null;
                }
            });
            assertNotNull(result, "Other thread had an error");
            assertTrue(result, "Expected true from other thread");

            // After commit.
            List<Label> labels = labelsStore.labelsForTriples(t);
            assertFalse(labels.isEmpty(), "Expected read-committed");
        });
    }
}
