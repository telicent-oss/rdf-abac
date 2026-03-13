package io.telicent.jena.abac.assembler;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.hashing.Hasher;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.rocks.TestLegacyLabelsStoreRocksDBByteBufferConfig;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.*;
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
import static io.telicent.jena.abac.labels.Labels.rocks;
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
            dbDirectory =
                    Files.createTempDirectory("tmp" + TestLegacyLabelsStoreRocksDBByteBufferConfig.class).toFile();
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
        assertRocksDBByString(store);
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
        assertRocksDBByString(store);
    }

    @ParameterizedTest
    @MethodSource("provideHasherAndName")
    public void test_generateStore_ByHash_happyPath(String hashName, Hasher expectedHash) throws RocksDBException {
        // given
        Resource r = model.createResource("test_generateStore_ByHash_" + hashName);
        r.addLiteral(pLabelsStoreByHash, true);
        r.addLiteral(pLabelsStoreByHashFunction, hashName);

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        // then
        assertNotNull(store);
        assertRocksDBByHash(store, expectedHash);

    }

    @Test
    public void test_generateStore_ByHash_missingAlgorithm_useDefault() throws RocksDBException {
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
    public void test_generateStore_ByHash_emptyAlgorithmString_useDefault() throws RocksDBException {
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
    public void test_generateStore_ByHash_wrongAlgorithmString_useDefault() throws RocksDBException {
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
                        .map(entry -> Arguments.of(entry.getKey(),
                                                   entry.getValue().get()));  // Provide both the key and the Hasher
    }

    public static Stream<Arguments> hashCombinations() {
        return Stream.of(Arguments.of(HasherUtil.XX_128, HasherUtil.XX_64),
                         Arguments.of(HasherUtil.XX_64, HasherUtil.CITY_64),
                         Arguments.of(HasherUtil.MURMUR_64, HasherUtil.MURMUR_128));
    }

    @ParameterizedTest(name = "Store Format Verification (Correct = {0}, Incorrect = {1})")
    @SuppressWarnings("resource")
    @MethodSource("hashCombinations")
    public void test_generateStore_withOneHash_reloadingWithAnotherHash_fails(String correct, String incorrect) throws
            Exception {
        // given
        Resource r = model.createResource("correct_hash");
        r.addLiteral(pLabelsStoreByHash, true);
        r.addLiteral(pLabelsStoreByHashFunction, correct);
        Resource r2 = model.createResource("incorrect_hash");
        r2.addLiteral(pLabelsStoreByHash, true);
        r2.addLiteral(pLabelsStoreByHashFunction, incorrect);

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        store.close();

        // then
        rocks.clear();
        Assertions.assertThrows(IllegalStateException.class, () -> LabelStoreAssembler.generateStore(dbDirectory, r2));
    }

}
