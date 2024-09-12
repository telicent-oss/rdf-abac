package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.AbstractTestLabelMatchPattern;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import io.telicent.jena.abac.labels.StoreFmt;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public abstract class BaseTestLabelMatchPatternRocksDB extends AbstractTestLabelMatchPattern {

    private File dbDirectory;

    @Override
    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        try {
            dbDirectory = Files.createTempDirectory("tmp" + storeFmt.getClass()).toFile();
            return Labels.createLabelsStoreRocksDB(dbDirectory, labelMode, null, storeFmt);
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
