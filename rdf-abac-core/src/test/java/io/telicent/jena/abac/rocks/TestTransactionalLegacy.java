package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.AbstractionTransactionalTests;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;

import java.nio.file.Files;

public class TestTransactionalLegacy extends AbstractionTransactionalTests {

    @Override
    @SuppressWarnings("deprecation")
    protected LabelsStore create() {
        try {
            return new LegacyLabelsStoreRocksDB(new RocksDBHelper(), Files.createTempDirectory("rocks").toFile(),
                                                new StoreFmtByHash(HasherUtil.createXX128Hasher()), null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
