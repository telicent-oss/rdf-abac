package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.StoreFmt;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import io.telicent.jena.abac.rocks.AbstractTestLabelMatchRocks;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.nio.file.Files;

public class AbstractTestLabelMatchModernRocks extends AbstractTestLabelMatchRocks {

    @Override
    protected LabelsStore createLabelsStore(StoreFmt storeFmt) {
        try {
            dbDirectory = Files.createTempDirectory("tmp" + storeFmt.getClass()).toFile();
            return new DictionaryLabelStoreRocksDB(dbDirectory, storeFmt);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }
}
