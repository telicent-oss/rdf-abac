package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import io.telicent.jena.abac.AbstractionTransactionalTests;

import java.nio.file.Files;

public class TestTransactionalModern extends AbstractionTransactionalTests {

    @Override
    protected LabelsStore create() {
        try {
            return new DictionaryLabelStoreRocksDB(Files.createTempDirectory("rocks").toFile(),
                                                   new StoreFmtByHash(HasherUtil.createXX128Hasher()));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
