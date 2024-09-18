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

import io.telicent.jena.abac.labels.*;
import io.telicent.jena.abac.labels.hashing.Hasher;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB.LabelMode;
import org.junit.jupiter.api.DisplayName;
import org.rocksdb.RocksDBException;

import static io.telicent.jena.abac.labels.hashing.HasherUtil.*;

/**
 * General tests run on a RocksDB backed label store
 */
public abstract class TestLabelStoreRocksDBGeneral extends AbstractTestLabelsStore {

    protected abstract StoreFmt createStoreFmt();

    protected LabelsStore createLabelsStoreRocksDB(File dbDir, LabelMode labelMode) throws RocksDBException {
        return Labels.createLabelsStoreRocksDB(dbDir, labelMode, null, createStoreFmt());
    }

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
        protected StoreFmt createStoreFmt() {
            return new StoreFmtByString();
        }
    }

    public static class ByNodeId extends TestLabelStoreRocksDBGeneral {
        @Override
        protected StoreFmt createStoreFmt() {
            return new StoreFmtByNodeId(new NaiveNodeTable());
        }
    }

    public static class ByNodeIdTrie extends TestLabelStoreRocksDBGeneral {
        @Override
        protected StoreFmt createStoreFmt() {
            return new StoreFmtByNodeId(new TrieNodeTable());
        }
    }

    public abstract static class ByHashAbstract extends TestLabelStoreRocksDBGeneral {
        @Override
        protected StoreFmt createStoreFmt() {
            return new StoreFmtByHash(getHasher());
        }

        abstract Hasher getHasher();


        @DisplayName("City64")
        public static class ByHash_City extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createCity64Hasher();
            }
        }

        @DisplayName("Sip24")
        public static  class ByHash_Sip24 extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createSIP24Hasher();
            }
        }

        @DisplayName("SHA256")
        public static class ByHash_SHA256 extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createSHA256Hasher();
            }
        }

        @DisplayName("SHA512")
        public static class ByHash_SHA512 extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createSHA512Hasher();
            }
        }

        @DisplayName("Metro64")
        public static class ByHash_Metro extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createMetro64Hasher();
            }
        }

        @DisplayName("Murmur64")
        public static class ByHash_Murmur extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createMurmur64Hasher();
            }
        }

        @DisplayName("Murmur128")
        public static class ByHash_Murmur128 extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createMurmer128Hasher();
            }
        }

        @DisplayName("Farm64")
        public static class ByHash_Farm64 extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createFarm64Hasher();
            }
        }

        @DisplayName("FarmNA")
        public static class ByHash_FarmNa extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createFarmNaHasher();
            }
        }

        @DisplayName("FarmUo")
        public static class ByHash_FarmUo extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createFarmUoHasher();
            }
        }

        @DisplayName("WY64")
        public static class ByHash_WY64 extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createWY64Hasher();
            }
        }

        @DisplayName("XX32")
        public static class ByHash_XX32 extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createXX32Hasher();
            }
        }

        @DisplayName("XX64")
        public static class ByHash_XX64 extends ByHashAbstract {
            @Override
            Hasher getHasher() {
                return createXX64Hasher();
            }
        }

    }
}
