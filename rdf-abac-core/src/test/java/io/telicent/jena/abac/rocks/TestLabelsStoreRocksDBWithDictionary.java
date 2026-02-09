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

import io.telicent.jena.abac.labels.*;
import io.telicent.smart.cache.storage.labels.DictionaryLabelsStore;
import org.junit.jupiter.params.provider.Arguments;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.util.stream.Stream;

/**
 * Tests for LabelsStoreRocksDB with dictionary-based label encoding.
 * Reuses the full AbstractTestLabelsStoreRocksDB test suite to verify that
 * dictionary encoding produces the same behaviour as text encoding.
 */
public class TestLabelsStoreRocksDBWithDictionary extends BaseTestLabelsStoreRocksDB {

    private DictionaryLabelsStore dictStore;

    public static Stream<Arguments> provideLabelAndStorageFmt() {
        return LabelAndStorageFormatProviderUtility.provideLabelAndStorageFmtByString();
    }

    @Override
    protected LabelsStore createLabelsStoreRocksDB(File dbDir, LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws RocksDBException {
        File dictDir = new File(dbDir, "dictionary");
        dictDir.mkdirs();
        dictStore = Labels.createDictionaryLabelsStore(dictDir, 1);
        return Labels.createLabelsStoreRocksDB(dbDir, labelMode, null, storeFmt, dictStore);
    }
}