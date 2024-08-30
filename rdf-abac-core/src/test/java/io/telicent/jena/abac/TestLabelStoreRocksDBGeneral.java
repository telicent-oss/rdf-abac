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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.node.table.NaiveNodeTable;
import io.telicent.jena.abac.labels.node.table.TrieNodeTable;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB.LabelMode;
import org.rocksdb.RocksDBException;

/** General tests run on a RocksDB backed label store */
public abstract class TestLabelStoreRocksDBGeneral extends AbstractTestLabelsStore {

    protected abstract LabelsStore createLabelsStoreRocksDB(File dbDir, LabelMode labelMode) throws RocksDBException;

    private static final LabelMode labelsMode = LabelMode.Overwrite;

    @Override
    protected LabelsStore createLabelsStore() {
        try {
            File dbDir = Files.createTempDirectory("tmpDirPrefix2").toFile();
            return createLabelsStoreRocksDB(dbDir, labelsMode);
        } catch (IOException | RocksDBException e) {
            throw new RuntimeException("Could not create RocksDB labels store", e);
        }
    }

    public static class ByString extends TestLabelStoreRocksDBGeneral {
        @Override
        protected LabelsStore createLabelsStoreRocksDB(File dbDir, LabelMode labelMode) throws RocksDBException {
                return Labels.createLabelsStoreRocksDBByString(dbDir, labelMode, null);
        }
    }

    public static class ById extends TestLabelStoreRocksDBGeneral {
        @Override
        protected LabelsStore createLabelsStoreRocksDB(File dbDir, LabelMode labelMode) throws RocksDBException {
                return Labels.createLabelsStoreRocksDBById(dbDir, new NaiveNodeTable(),labelMode, null);
        }
    }

    public static class ByIdTrie extends TestLabelStoreRocksDBGeneral {
        @Override
        protected LabelsStore createLabelsStoreRocksDB(File dbDir, LabelMode labelMode) throws RocksDBException {
                return Labels.createLabelsStoreRocksDBById(dbDir, new TrieNodeTable(), labelMode, null);
        }
    }
}
