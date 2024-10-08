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

package io.telicent.jena.abac.rocks;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

/**
 * RocksDB labels store
 */
@Suite
@SelectClasses({
        TestLabelsStoreRocksDBByString.class
        , TestLabelsStoreRocksDBByNodeId.class
        , TestLabelsStoreRocksDBByHash.class


        , TestLabelMatchRocksDBByString.class
        , TestLabelMatchRocksDBByNodeId.class
        , TestLabelMatchRocksDBByHash.class
        // Keep the pattern matching functionality in Label store RocksDB alive for now.
        , TestLabelMatchPatternRocksDBByString.class
        , TestLabelMatchPatternRocksDBByNodeId.class


        // Consistency checking.
        // Run Rocks tests on the separate in-memory label store.
        , TestLabelsStoreMemGraphRocksDB.class
        , TestLabelMatchMem.class

        , TestLabelsRocksDBNormalization.class
})
public class TS_LabelStoreRocksDB {
}
