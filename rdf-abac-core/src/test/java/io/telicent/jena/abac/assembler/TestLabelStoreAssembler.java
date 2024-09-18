package io.telicent.jena.abac.assembler;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.hashing.Hasher;
import io.telicent.jena.abac.rocks.TestLabelsStoreRocksDBByteBufferConfig;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import static io.telicent.jena.abac.core.VocabAuthzDataset.*;
import static io.telicent.jena.abac.labels.Labels.closeLabelsStoreRocksDB;
import static io.telicent.jena.abac.labels.LabelsStoreRocksDB.LabelMode.Merge;
import static io.telicent.jena.abac.labels.LabelsStoreRocksDB.LabelMode.Overwrite;
import static io.telicent.jena.abac.labels.TestStoreFmt.assertRocksDBByHash;
import static io.telicent.jena.abac.labels.TestStoreFmt.assertRocksDBByString;
import static io.telicent.jena.abac.labels.hashing.HasherUtil.createXX128Hasher;
import static io.telicent.jena.abac.labels.hashing.HasherUtil.hasherMap;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestLabelStoreAssembler {
    static File dbDirectory;
    Model model;
    LabelsStore store;

    @BeforeEach
    public void setUpFiles() {
        model = ModelFactory.createDefaultModel();
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
        closeLabelsStoreRocksDB(store);
    }

    @Test
    public void test_generateStore_default_ByString_Overwrite() throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_default_ByString_Overwrite");
        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
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
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByString(store,Merge);
    }

    @ParameterizedTest
    @MethodSource("provideHasherAndName")
    public void test_generateStore_ByHash_happyPath(String hashName, Hasher expectedHash) throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByHash_"+hashName);
        r.addLiteral(pLabelsStoreByHash, true);
        r.addLiteral(pLabelsStoreByHashFunction, hashName);

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByHash(store, expectedHash);

    }

    @Test
    public void test_generateStore_ByHash_missingAlgorithm_useDefault () throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByHash_missingAlgorithm");
        r.addLiteral(pLabelsStoreByHash, true);

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByHash(store, createXX128Hasher());
    }

    @Test
    public void test_generateStore_ByHash_emptyAlgorithmString_useDefault () throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByHash_missingAlgorithm");
        r.addLiteral(pLabelsStoreByHash, true);
        r.addLiteral(pLabelsStoreByHashFunction, "");

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByHash(store, createXX128Hasher());
    }

    @Test
    public void test_generateStore_ByHash_wrongAlgorithmString_useDefault () throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByHash_missingAlgorithm");
        r.addLiteral(pLabelsStoreByHash, true);
        r.addLiteral(pLabelsStoreByHashFunction, "MISSING");

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByHash(store, createXX128Hasher());
    }

    /**
     * This method provides the hashers and names from the HasherUtils mapping
     */
    protected static Stream<Arguments> provideHasherAndName() {
        // Convert the hasherMap's entries (key-value pairs) to a Stream of Arguments
        return hasherMap.entrySet().stream()
                .map(entry -> Arguments.of(entry.getKey(), entry.getValue().get()));  // Provide both the key and the Hasher
    }

}
