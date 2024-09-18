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

import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.hashing.Hasher;
import io.telicent.jena.abac.rocks.LabelAndStorageFormatProviderUtility;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.telicent.jena.abac.labels.hashing.HasherUtil.hasherMap;

/**
 * Run {@link BulkDirectory} tests using the hash-based RocksDB label store,
 * using a setup extension which creates the right kind of store.
 * Due to the additional needs of a Hasher implementation we disable the tests and re-implement them
 * to allow us to set up the relevant hasher as acquired from the HasherUtil map.
 */
@ExtendWith(RocksDBSetupExtension.class)
public class BulkDirectoryRocksDBTestsByHash extends AbstractBulkDirectoryRocksDB {

    public static Stream<Arguments> provideLabelAndStorageFmt() {
        return LabelAndStorageFormatProviderUtility.provideLabelAndStorageFmtByHash();
    }

    public static Stream<Arguments> provideStorageFmt() {
        // Get a stream of Hashers from the hasherMap
        Stream<Hasher> hasherStream = hasherMap.values().stream()
                .map(Supplier::get);  // Get each Hasher from the Supplier

        // For each hasher, create a StorageFmtByHash and return it as an argument
        return hasherStream.map(hasher -> {
            // Create a new instance of StorageFmtByHash with the hasher
            StoreFmtByHash storageFmtHash = new StoreFmtByHash(hasher);
            // Return only the StorageFmtByHash as an argument
            return Arguments.of(storageFmtHash);
        });
    }

}
