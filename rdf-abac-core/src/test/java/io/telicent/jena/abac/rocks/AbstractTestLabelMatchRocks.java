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
import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Concrete triple pattern testing (no wildcards) with all Rocks modes
 */
@SuppressWarnings("deprecation")
public abstract class AbstractTestLabelMatchRocks {

    private static final Node s = SSE.parseNode(":s");
    private static final Node p = SSE.parseNode(":p");
    private static final Node o = SSE.parseNode(":o");

    protected File dbDirectory;

    protected LabelsStore createLabelsStore( StoreFmt storeFmt) {
        try {
            dbDirectory = Files.createTempDirectory("tmp" + storeFmt.getClass()).toFile();
            return Labels.createLabelsStoreRocksDB(dbDirectory, null, storeFmt);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    static Stream<Arguments> provideStorageFormat() {
        return Stream.of(Arguments.of(null));
    }

    LabelsStore createStore( StoreFmt storeFmt) {
        LabelsStore labels = createLabelsStore(storeFmt);
        labels.add(s, p, o, Label.fromText("spo"));
        return labels;
    }

    @AfterEach void destroyStore() throws IOException {
        FileUtils.deleteDirectory(dbDirectory);
        dbDirectory = null;
        Labels.rocks.clear();
    }

    static Triple triple(String string) { return SSE.parseTriple(string); }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void label_match_basic( StoreFmt storeFmt) throws Exception {
        try(LabelsStore emptyLabelStore = createLabelsStore(storeFmt)) {
            Triple t = triple("(:s1 :p1 :o1)");
            Label x = emptyLabelStore.labelForTriple(t);
            assertNull(x);
        }
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void label_match_spo( StoreFmt storeFmt) throws Exception {
        try(LabelsStore ls = createStore(storeFmt)) {
            match(s, p, o, ls, Label.fromText("spo"));
        }
    }

    private void match(Node s, Node p, Node o, LabelsStore labels, Label expected) {
        Triple triple = Triple.create(s, p, o);
        Label actual = labels.labelForTriple(triple);
        Assertions.assertEquals(expected, actual);
    }
}
