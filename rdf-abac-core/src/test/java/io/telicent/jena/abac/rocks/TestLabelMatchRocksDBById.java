package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import io.telicent.jena.abac.labels.node.table.NaiveNodeTable;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestLabelMatchRocksDBById extends AbstractTestLabelMatchRocks {

    private File dbDirectory;

    @Override
    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode) {
        try {
            dbDirectory = Files.createTempDirectory("tmp" + TestLabelMatchRocksDBByString.class).toFile();
            return Labels.createLabelsStoreRocksDBById(dbDirectory, new NaiveNodeTable(), labelMode, null);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @Override
    protected void destroyLabelsStore(LabelsStore labels) {
        dbDirectory.delete();
        dbDirectory = null;
    }
}
