package io.telicent.jena.abac.assembler;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import org.apache.commons.io.FileUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.stream.Stream;

import static io.telicent.jena.abac.core.VocabAuthzDataset.*;
import static io.telicent.jena.abac.labels.Labels.closeLabelsStoreRocksDB;
import static io.telicent.jena.abac.labels.Labels.rocks;

public class TestLabelStoreAssemblerModern {
    static File dbDirectory;
    Model model;
    LabelsStore store;

    @BeforeEach
    public void setUpFiles() {
        model = ModelFactory.createDefaultModel();
        try {
            dbDirectory =
                    Files.createTempDirectory("tmp").toFile();
        } catch (IOException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @AfterEach
    public void tearDownFiles() {
        Labels.rocks.clear();
        closeLabelsStoreRocksDB(store);
        try {
            FileUtils.deleteDirectory(dbDirectory);
        } catch (IOException e) {
            // Ignored, we're creating a new temporary directory each time so don't care if we don't clean up this
            // directory fully
        }
    }

    private void verifyModernRocksDbUsed(LabelsStore store) {
        Assertions.assertInstanceOf(DictionaryLabelStoreRocksDB.class, store);
    }

    /**
     * Provides different combinations of hash function names to verify that if we create a store with one hash
     * function, then try to reopen it with another, we successfully detect that a different hash function is in use and
     * refuse to proceed.
     * <p>
     * Note some combinations are intentionally two different lengths of hash within the same family.  This explores a
     * problem discovered during testing that previously we "named" our hash functions based on the implementation class
     * name.  For some hashes within the same family this was actually identical so we couldn't use this as a reliable
     * unique identifier.  As of {@code 3.0.0} we instead use the name constants as the names for the hashes, rather
     * than their implementation classes, to avoid this problem.
     * </p>
     *
     * @return Various hash combinations
     */
    public static Stream<Arguments> hashCombinations() {
        return Stream.of(Arguments.of(HasherUtil.XX_128, HasherUtil.XX_64),
                         Arguments.of(HasherUtil.XX_64, HasherUtil.CITY_64),
                         Arguments.of(HasherUtil.WY_3, HasherUtil.SIP_24),
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
        r.addLiteral(pLabelsStoreLegacy, false);
        Resource r2 = model.createResource("incorrect_hash");
        r2.addLiteral(pLabelsStoreByHash, true);
        r2.addLiteral(pLabelsStoreByHashFunction, incorrect);
        r2.addLiteral(pLabelsStoreLegacy, false);

        // when
        store = LabelStoreAssembler.generateStore(dbDirectory, r);
        verifyModernRocksDbUsed(store);
        store.close();

        // then
        rocks.clear();
        Assertions.assertThrows(IllegalStateException.class, () -> LabelStoreAssembler.generateStore(dbDirectory, r2));
    }
}
