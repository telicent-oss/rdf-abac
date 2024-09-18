package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.labels.*;
import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RocksDB store specific tests.
 */
public abstract class BaseTestLabelsStoreRocksDB extends AbstractTestLabelsStoreRocksDB {

    protected File dbDir;

    protected LabelsStore createLabelsStoreRocksDB(final File dbDir, final LabelsStoreRocksDB.LabelMode labelMode, final StoreFmt storeFmt) throws RocksDBException {
        return Labels.createLabelsStoreRocksDB(dbDir, labelMode, null, storeFmt);
    }

    @Override
    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        try {
            dbDir = Files.createTempDirectory("tmpDirPrefix").toFile();
            store = createLabelsStoreRocksDB(dbDir, labelMode, storeFmt);
            return store;
        } catch (IOException | RocksDBException e) {
            throw new RuntimeException("Could not create RocksDB labels store", e);
        }
    }

    protected LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt, Graph graph) {
        throw new RuntimeException("RocksDB labels store does not support graphs");
    }

    protected void deleteLabelsStore() {
        try {
            FileUtils.deleteDirectory(dbDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete RocksDB labels store", e);
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_closed(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        Labels.closeLabelsStoreRocksDB(store);
        assertThrows(RuntimeException.class, () -> {
            List<String> x = store.labelsForTriples(triple1);
        });
    }

    /**
     * It seems more correct to throw an RTE than to return null
     */
    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void labelsStore_get_wild(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) {
        store = createLabelsStore(labelMode, storeFmt);
        store.add(triple1, "label1");
        store.add(triple2, "label2");
        assertThrows(RuntimeException.class, () -> {
            List<String> x = store.labelsForTriples(Triple.ANY);  //warning
        });
        assertThrows(RuntimeException.class, () -> {
            List<String> x = store.labelsForTriples(Triple.create(triple1.getSubject(), triple1.getObject(), Node.ANY));  //warning
        });
    }
}
