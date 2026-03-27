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
import io.telicent.jena.abac.labels.StoreFmt;
import io.telicent.jena.abac.rocks.StorageFormatProviderUtility;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Run {@link BulkDirectory} tests using the non-RocksDB label store,
 * using a setup extension which creates the right kind of store.
 */
public class BulkDirectoryMemTests extends BulkDirectory {

    @Override
    LabelsStore createLabelsStore(StoreFmt storeFmt) {
        return Labels.createLabelsStoreMem();
    }

    public static Stream<Arguments> provideStorageFormat() {
        return StorageFormatProviderUtility.provideStorageFormatsByString();
    }
}
