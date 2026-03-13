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

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RocksDB store specific tests.
 */
@SuppressWarnings("deprecation")
public abstract class BaseTestLegacyLabelsStoreRocksDB extends AbstractTestLegacyLabelsStoreRocksDB {

    protected File dbDir;

    protected LabelsStore createLabelsStoreRocksDB(final File dbDir, final StoreFmt storeFmt) throws RocksDBException {
        return Labels.createLabelsStoreRocksDB(dbDir, null, storeFmt);
    }

    @Override
    protected LabelsStore createLabelsStore(StoreFmt storeFmt) {
        try {
            dbDir = Files.createTempDirectory("tmpDirPrefix").toFile();
            store = createLabelsStoreRocksDB(dbDir, storeFmt);
            return store;
        } catch (IOException | RocksDBException e) {
            throw new RuntimeException("Could not create RocksDB labels store", e);
        }
    }

    protected LabelsStore createLabelsStore(StoreFmt storeFmt, Graph graph) {
        throw new RuntimeException("RocksDB labels store does not support graphs");
    }

    protected void deleteLabelsStore() {
        try {
            FileUtils.deleteDirectory(dbDir);
        } catch (IOException e) {
            throw new RuntimeException("Could not delete RocksDB labels store", e);
        }
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void labelsStore_closed( StoreFmt storeFmt) {
        store = createLabelsStore(storeFmt);
        Labels.closeLabelsStoreRocksDB(store);
        assertThrows(RuntimeException.class, () -> {
            Label x = store.labelForTriple(triple1);
        });
    }

    /**
     * It seems more correct to throw an RTE than to return null
     */
    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void labelsStore_get_wild( StoreFmt storeFmt) {
        store = createLabelsStore(storeFmt);
        store.add(triple1, Label.fromText("label1"));
        store.add(triple2, Label.fromText("label2"));
        assertThrows(RuntimeException.class, () -> {
            Label x = store.labelForTriple(Triple.ANY);  //warning
        });
        assertThrows(RuntimeException.class, () -> {
            Label x = store.labelForTriple(Triple.create(triple1.getSubject(), triple1.getObject(), Node.ANY));  //warning
        });
    }
}
