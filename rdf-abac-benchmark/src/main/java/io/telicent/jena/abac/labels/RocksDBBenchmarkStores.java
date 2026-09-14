package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.nio.file.Files;

/** Factory for dictionary labels stores used by benchmarks. */
public final class RocksDBBenchmarkStores {
    private RocksDBBenchmarkStores() {}

    public static DictionaryLabelStoreRocksDB buildLabelsStoreRocksDB() throws IOException, RocksDBException {
        return new DictionaryLabelStoreRocksDB(Files.createTempDirectory("benchmark").toFile(),
                                              new StoreFmtByHash(HasherUtil.createXX128Hasher()));
    }
}
