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

/**
 * Run tests using the non-RocksDB label store.
 */
@SuppressWarnings("deprecation")
public class TestLabelMatchMem extends AbstractTestLabelMatchRocks {
    @Override
    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        // ignore the label mode - this store doesn't have modes
        return LabelsStoreMemPattern.create();
    }

    @Override
    void destroyStore() {}
}
