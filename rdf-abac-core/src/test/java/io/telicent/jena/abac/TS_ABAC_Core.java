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

import io.telicent.jena.abac.attributes.syntax.tokens.TestTokenizerABAC;
import io.telicent.jena.abac.core.*;
import io.telicent.jena.abac.labels.TestStoreFmtByNodeId;
import io.telicent.jena.abac.labels.TestStoreFmtByString;
import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({
    TestAuthMisc.class

    // Component testing.
    , TestAttributeParser.class
    , TestAttributeExprList.class
    , TestAttributeExprParse.class
    , TestHierarchy.class

    , TestAttributeValue.class
    , TestAttributeValueSet.class
    , TestAttributeValueList.class

    , TestAttributeExprEval.class
    , TestLabelsMem.class
    , TestLabelsMemPattern.class
    , TestLabelsMemNoPatterns.class
    , TestAssemblerABAC.class

    , TestLabelsStoreMem.class
    , TestAE.class
    , TestABAC.class
    , TestCtxABAC.class
    , TestAttributeStoreRemote.class
    , TestAttributeStoreLocal.class
    , TestAttributeStoreCache.class
    , TestAttributes.class
    , TestTokenizerABAC.class

    // RocksDB related.
    , TestStoreFmtByString.class
    , TestStoreFmtByNodeId.class

    , TestLabelStoreRocksDBGeneral.ByString.class
    , TestLabelStoreRocksDBGeneral.ByNodeId.class
    , TestLabelStoreRocksDBGeneral.ByNodeIdTrie.class

    /*
     * These tests are split because it seems RocksDB does not completely clear up fast enough within one suite.
     * Tests on their own we stable, but the suite is not if this test below is included.
     */
    , TestDatasetPersistentLabelsABAC.class
    , TestDatasetPersistentLabelsABAC2.class
})

public class TS_ABAC_Core {}
