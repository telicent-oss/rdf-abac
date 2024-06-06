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
package io.telicent.jena.abac.bulk;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rocksdb.RocksDBException;

/**
 * Run {@link BulkDirectory} tests using the string-based RocksDB label store,
 * using a setup extension which creates the right kind of store.
 */
@ExtendWith(RocksDBSetupExtension.class)
public class BulkDirectoryRocksDBTestsByString extends AbstractBulkDirectoryRocksDB {

    @Override
    LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode) throws RocksDBException {
        return Labels.createLabelsStoreRocksDBByString(dbDir, labelMode);
    }
}
