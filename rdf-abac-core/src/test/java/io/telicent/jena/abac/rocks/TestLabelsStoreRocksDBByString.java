package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;

public class TestLabelsStoreRocksDBByString extends BaseTestLabelsStoreRocksDB {
    @Override
    protected LabelsStore createLabelsStoreRocksDB(File dbDir, LabelsStoreRocksDB.LabelMode labelMode) throws RocksDBException {
        return Labels.createLabelsStoreRocksDBByString(dbDir, labelMode, null);
    }
}
