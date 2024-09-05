package io.telicent.jena.abac.assembler;

import io.telicent.jena.abac.labels.*;
import io.telicent.jena.abac.rocks.TestLabelsStoreRocksDBByteBufferConfig;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static io.telicent.jena.abac.core.VocabAuthzDataset.*;
import static io.telicent.jena.abac.labels.LabelsStoreRocksDB.LabelMode.Merge;
import static io.telicent.jena.abac.labels.LabelsStoreRocksDB.LabelMode.Overwrite;
import static io.telicent.jena.abac.labels.TestStoreFmt.assertRocksDBById;
import static io.telicent.jena.abac.labels.TestStoreFmt.assertRocksDBByString;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestLabelStoreAssembler {
    static File dbDirectory;
    static Model model = ModelFactory.createDefaultModel();

    @BeforeEach
    public void setUpFiles() {
        try {
            dbDirectory = Files.createTempDirectory("tmp" + TestLabelsStoreRocksDBByteBufferConfig.class).toFile();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @AfterEach
    public void tearDownFiles() {
        Labels.rocks.clear();
        dbDirectory.delete();
    }

    @Test
    public void test_generateStore_default_ByString_Overwrite() throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_default_ByString_Overwrite");
        // when
        LabelsStore store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByString(store, Overwrite);
    }

    @Test
    public void test_generateStore_ByStringMerge() throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByStringMerge");
        r.addLiteral(pLabelsStoreUpdateModeMerge, true);
        r.addLiteral(pLabelsStoreByString, true);
        // when
        LabelsStore store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByString(store,Merge);
    }

    @Test
    public void test_generateStore_ByIDOverwrite() throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByIDOverwrite");
        r.addLiteral(pLabelsStoreByID, true);
        r.addLiteral(pLabelsStoreUpdateModeOverwrite, true);
        // when
        LabelsStore store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBById(store,Overwrite);
    }

    @Test
    public void test_generateStore_ByTrieOverwrite() throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByTrieOverwrite");
        r.addLiteral(pLabelsStoreByTrie, true);
        r.addLiteral(pLabelsStoreUpdateModeOverwrite, true);
        // when
        LabelsStore store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        // Note: we can't differentiate between Trie/ID
        assertRocksDBById(store,Overwrite);
    }

}
