package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.system.Txn;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.nio.file.Files;

public class TestTransactions {

    private static final Triple SAMPLE = SSE.parseTriple("(:s :p :o)");

    private static DictionaryLabelStoreRocksDB createStore() throws IOException, RocksDBException {
        return new DictionaryLabelStoreRocksDB(Files.createTempDirectory("rocks").toFile(),
                new StoreFmtByHash(HasherUtil.createXX128Hasher()));
    }

    private static LegacyLabelsStoreRocksDB createLegacyStore() throws IOException {
        return new LegacyLabelsStoreRocksDB(new RocksDBHelper(), Files.createTempDirectory("rocks").toFile(),
                new StoreFmtByHash(HasherUtil.createXX128Hasher()), null);
    }

    @Test
    public void end_after_commit() throws Exception {
        // legacy works
        try (LegacyLabelsStoreRocksDB store = createLegacyStore()) {
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.WRITE);
            store.add(SAMPLE, Label.fromText("public"));
            transactional.commit();
            transactional.end();
        }
        // This throws "java.lang.IllegalStateException: Not in a transaction"
        try (DictionaryLabelStoreRocksDB store = createStore()) {
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.WRITE);
            store.add(SAMPLE, Label.fromText("public"));
            transactional.commit();
            transactional.end();
        }
    }

    /*
        Works out the same way
     */
    @Test
    public void execute_write() throws Exception {
        // Legacy works
        try (LegacyLabelsStoreRocksDB store = createLegacyStore()) {
            Txn.executeWrite(store.getTransactional(),
                    () -> store.add(SAMPLE,
                            Label.fromText("public")));
        }
        // This also throws "java.lang.IllegalStateException: Not in a transaction"
        try (DictionaryLabelStoreRocksDB store = createStore()) {
            Txn.executeWrite(store.getTransactional(),
                    () -> store.add(SAMPLE,
                            Label.fromText("public")));
        }
    }
}