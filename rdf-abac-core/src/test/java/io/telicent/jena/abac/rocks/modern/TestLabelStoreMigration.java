package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.StoreFmt;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.StoreFmtByString;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@SuppressWarnings("deprecation")
public class TestLabelStoreMigration {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestLabelStoreMigration.class);

    private static final Triple t1 = SSE.parseTriple("(:s :p 123)");
    private static final Triple t2 = SSE.parseTriple("(:s :p 'test')");

    private static final Label l1 = Label.fromText("public");
    private static final Label l2 = Label.fromText("admin && employee");
    public static final String REAL_TEST_DATA = "src/test/files/labels/legacy-labels.zip";

    public static Stream<Arguments> storeFormats() {
        return Stream.of(Arguments.of(new StoreFmtByString(), new StoreFmtByHash(HasherUtil.createMurmer128Hasher())),
                         Arguments.of(new StoreFmtByString(), new StoreFmtByHash(HasherUtil.createXX128Hasher())),
                         Arguments.of(new StoreFmtByHash(HasherUtil.createXX128Hasher()),
                                      new StoreFmtByHash(HasherUtil.createXX128Hasher())));
    }

    @ParameterizedTest
    @MethodSource("storeFormats")
    public void givenPopulatedLegacyStore_whenOpeningWithModernStore_thenDataAutomaticallyMigrated(StoreFmt source,
                                                                                                   StoreFmt target) throws
            IOException, RocksDBException {
        // Given
        File dbDir = Files.createTempDirectory("rocks").toFile();
        try (LegacyLabelsStoreRocksDB legacyStore = new LegacyLabelsStoreRocksDB(new RocksDBHelper(), dbDir, source,
                                                                                 null)) {
            legacyStore.getTransactional().begin(TxnType.WRITE);
            legacyStore.add(t1, l1);
            legacyStore.add(t2, l2);
            legacyStore.getTransactional().commit();
        }

        // When
        try (DictionaryLabelStoreRocksDB modernStore = new DictionaryLabelStoreRocksDB(dbDir, target)) {
            Assertions.assertNotNull(modernStore);

            // Then
            Assertions.assertEquals(l1, modernStore.labelForTriple(t1));
            Assertions.assertEquals(l2, modernStore.labelForTriple(t2));
        }
    }

    public static Stream<Arguments> storeFormatsWithSizes() {
        return storeFormats().flatMap(fmts -> Arrays.stream(new int[] { 10, 100, 1_000, 10_000 })
                                                    .mapToObj(
                                                            size -> Arguments.of(fmts.get()[0], fmts.get()[1], size)));
    }

    @ParameterizedTest(name = "Legacy Data Migration (Source = {0}, Target = {1}, Data Size = {2})")
    @MethodSource("storeFormatsWithSizes")
    public void givenRandomlyPopulatedLegacyStoreWithFullyRandomLabels_whenOpeningWithModernStore_thenDataAutomaticallyMigrated(
            StoreFmt source, StoreFmt target, int size) throws IOException, RocksDBException {
        // Given
        File dbDir = Files.createTempDirectory("rocks").toFile();
        List<Triple> triples = new ArrayList<>();
        List<Label> labels = new ArrayList<>();
        try (LegacyLabelsStoreRocksDB legacyStore = new LegacyLabelsStoreRocksDB(new RocksDBHelper(), dbDir, source,
                                                                                 null)) {
            legacyStore.getTransactional().begin(TxnType.WRITE);
            for (int i = 1; i <= size; i++) {
                Triple t = SSE.parseTriple(("(:s :p" + RandomUtils.insecure().randomInt(1, 10) + " '" + i + "')"));
                triples.add(t);
                Label l = Label.fromText(RandomStringUtils.insecure().nextAlphabetic(50));
                labels.add(l);
                legacyStore.add(t, l);
            }
            legacyStore.getTransactional().commit();
        }
        long sizeBefore = FileUtils.sizeOfDirectory(dbDir);

        // When
        try (DictionaryLabelStoreRocksDB modernStore = new DictionaryLabelStoreRocksDB(dbDir, target)) {
            Assertions.assertNotNull(modernStore);

            // Then
            for (int i = 0; i < triples.size(); i++) {
                Triple t = triples.get(i);
                Label expected = labels.get(i);

                Assertions.assertEquals(expected, modernStore.labelForTriple(t), "Wrong label for triple " + i);
            }
        }
        long sizeAfter = FileUtils.sizeOfDirectory(dbDir);
        reportSizes(sizeBefore, sizeAfter);
    }

    @ParameterizedTest(name = "Legacy Data Migration (Source = {0}, Target = {1}, Data Size = {2})")
    @MethodSource("storeFormatsWithSizes")
    public void givenRandomlyPopulatedLegacyStoreWithRepeatingLabels_whenOpeningWithModernStore_thenDataAutomaticallyMigrated(
            StoreFmt source, StoreFmt target, int size) throws IOException, RocksDBException {
        // Given
        File dbDir = Files.createTempDirectory("rocks").toFile();
        List<Triple> triples = new ArrayList<>();
        List<Label> labelPool = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            labelPool.add(Label.fromText(RandomStringUtils.insecure().nextAlphabetic(50)));
        }
        List<Label> labels = new ArrayList<>();
        try (LegacyLabelsStoreRocksDB legacyStore = new LegacyLabelsStoreRocksDB(new RocksDBHelper(), dbDir, source,
                                                                                 null)) {
            legacyStore.getTransactional().begin(TxnType.WRITE);
            for (int i = 1; i <= size; i++) {
                Triple t = SSE.parseTriple(("(:s :p" + RandomUtils.insecure().randomInt(1, 10) + " '" + i + "')"));
                triples.add(t);
                Label l = labelPool.get(RandomUtils.insecure().randomInt(0, labelPool.size()));
                labels.add(l);
                legacyStore.add(t, l);
            }
            legacyStore.getTransactional().commit();
        }
        long sizeBefore = FileUtils.sizeOfDirectory(dbDir);

        // When
        try (DictionaryLabelStoreRocksDB modernStore = new DictionaryLabelStoreRocksDB(dbDir, target)) {
            Assertions.assertNotNull(modernStore);

            // Then
            for (int i = 0; i < triples.size(); i++) {
                Triple t = triples.get(i);
                Label expected = labels.get(i);

                Assertions.assertEquals(expected, modernStore.labelForTriple(t), "Wrong label for triple " + i);
            }
        }
        long sizeAfter = FileUtils.sizeOfDirectory(dbDir);
        reportSizes(sizeBefore, sizeAfter);
    }

    @ParameterizedTest(name = "Legacy Data Migration (Source = {0}, Target = {1}, Data Size = {2})")
    @MethodSource("storeFormatsWithSizes")
    public void givenRandomlyPopulatedLegacyStoreWithSingleLabel_whenOpeningWithModernStore_thenDataAutomaticallyMigrated(
            StoreFmt source, StoreFmt target, int size) throws IOException, RocksDBException {
        // Given
        File dbDir = Files.createTempDirectory("rocks").toFile();
        List<Triple> triples = new ArrayList<>();
        Label label = Label.fromText(RandomStringUtils.insecure().nextAlphabetic(256));
        try (LegacyLabelsStoreRocksDB legacyStore = new LegacyLabelsStoreRocksDB(new RocksDBHelper(), dbDir, source,
                                                                                 null)) {
            legacyStore.getTransactional().begin(TxnType.WRITE);
            for (int i = 1; i <= size; i++) {
                Triple t = SSE.parseTriple(("(:s :p" + RandomUtils.insecure().randomInt(1, 10) + " '" + i + "')"));
                triples.add(t);
                legacyStore.add(t, label);
            }
            legacyStore.getTransactional().commit();
        }
        long sizeBefore = FileUtils.sizeOfDirectory(dbDir);

        // When
        try (DictionaryLabelStoreRocksDB modernStore = new DictionaryLabelStoreRocksDB(dbDir, target)) {
            Assertions.assertNotNull(modernStore);

            // Then
            for (int i = 0; i < triples.size(); i++) {
                Triple t = triples.get(i);
                Assertions.assertEquals(label, modernStore.labelForTriple(t), "Wrong label for triple " + i);
            }
        }
        long sizeAfter = FileUtils.sizeOfDirectory(dbDir);
        reportSizes(sizeBefore, sizeAfter);
    }

    private static void reportSizes(long sizeBefore, long sizeAfter) {
        System.out.format("Legacy Store size: %,d bytes\n", sizeBefore);
        System.out.format("Modern Store size: %,d bytes\n", sizeAfter);
        System.out.format("Size Difference: %,d bytes\n", sizeAfter - sizeBefore);
    }

    @Test
    public void givenRealLegacyStore_whenOpeningWithModernStore_thenDataAutomaticallyMigrated() throws IOException,
            RocksDBException {
        // Given
        Path backupDir = Files.createTempDirectory("rocks-backup");
        Path dbDir = Files.createTempDirectory("rocks");
        unpackZippedData(REAL_TEST_DATA, backupDir, "ontology");
        try (LegacyLabelsStoreRocksDB legacyStore = new LegacyLabelsStoreRocksDB(new RocksDBHelper(), dbDir.toFile(),
                                                                                 new StoreFmtByString(),
                                                                                 null)) {
            legacyStore.restore(backupDir.toFile().getAbsolutePath());
            Assertions.assertFalse(legacyStore.isEmpty());
        }
        long sizeBefore = FileUtils.sizeOfDirectory(dbDir.toFile());

        // When
        try (DictionaryLabelStoreRocksDB modernStore = new DictionaryLabelStoreRocksDB(dbDir.toFile(),
                                                                                       new StoreFmtByHash(
                                                                                               HasherUtil.createXX128Hasher()))) {
            // Then
            Assertions.assertFalse(modernStore.isEmpty());
        }
        long sizeAfter = FileUtils.sizeOfDirectory(dbDir.toFile());
        reportSizes(sizeBefore, sizeAfter);
    }

    /**
     * Unzips a backup archive from a real store
     *
     * @param testDataArchive Test data archive, a backup ZIP file taken from Smart Cache Graph
     * @param unpackDir       Directory to unpack into
     * @param dataset         Dataset to filter for i.e. only the labels backup from this dataset will be unpacked to
     *                        the unpack directory
     * @throws IOException Thrown if the data cannot be unzipped
     */
    private static void unpackZippedData(String testDataArchive, Path unpackDir, String dataset) throws IOException {
        int unpacked = 0;
        LOGGER.info("Unpacking {} dataset from ZIP archive {} with size {} bytes", dataset, testDataArchive,
                    new File(testDataArchive).length());
        try (ArchiveInputStream<ZipArchiveEntry> i = new ZipArchiveInputStream(
                new FileInputStream(testDataArchive))) {
            ZipArchiveEntry entry = null;
            while ((entry = i.getNextEntry()) != null) {
                if (!i.canReadEntryData(entry)) {
                    // log something?
                    continue;
                }
                if (!StringUtils.contains(entry.getName(), "/labels")) {
                    continue;
                }
                File f = entry.resolveIn(unpackDir).toFile();
                if (StringUtils.contains(f.getAbsolutePath(), "/" + dataset + "/labels")) {
                    f = new File(f.getAbsolutePath().replace("/" + dataset + "/labels", ""));
                }
                if (entry.isDirectory()) {
                    if (!f.isDirectory() && !f.mkdirs()) {
                        throw new IOException("failed to create directory " + f);
                    }
                } else {
                    File parent = f.getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IOException("failed to create directory " + parent);
                    }
                    try (OutputStream o = Files.newOutputStream(f.toPath())) {
                        IOUtils.copy(i, o);
                        unpacked++;
                    }
                }
            }
        }
        LOGGER.info("Completing unpacking {} dataset from ZIP archive {} with {} files unpacked", dataset,
                    testDataArchive, unpacked);
        Assertions.assertTrue(unpacked > 0, "No files unpacked from test data, was dataset name correct?");
    }

    @Test
    public void givenLargeLegacyStore_whenOpeningWithModernStore_thenDataAutomaticallyMigrated_andOpeningAgainDoesNotRepeatMigration() throws IOException,
            RocksDBException {
        // Given
        String largeTestData = System.getProperty("large-test-data");
        Assumptions.assumeTrue(StringUtils.isNotBlank(largeTestData));

        Path backupDir = Files.createTempDirectory("rocks-backup");
        Path dbDir = Files.createTempDirectory("rocks");
        unpackZippedData(largeTestData, backupDir, "knowledge");
        try (LegacyLabelsStoreRocksDB legacyStore = new LegacyLabelsStoreRocksDB(new RocksDBHelper(), dbDir.toFile(),
                                                                                 new StoreFmtByString(),
                                                                                 null)) {
            legacyStore.restore(backupDir.toFile().getAbsolutePath());
            Assertions.assertFalse(legacyStore.isEmpty());
        }
        long sizeBefore = FileUtils.sizeOfDirectory(dbDir.toFile());

        // When
        try (DictionaryLabelStoreRocksDB modernStore = new DictionaryLabelStoreRocksDB(dbDir.toFile(),
                                                                                       new StoreFmtByHash(
                                                                                               HasherUtil.createXX128Hasher()))) {
            // Then
            Assertions.assertFalse(modernStore.isEmpty());
            System.out.println("Unique Labels: " + modernStore.labelCount());
            System.out.println("Labelled Quads: " + modernStore.keyCount());
        }
        long sizeAfter = FileUtils.sizeOfDirectory(dbDir.toFile());
        reportSizes(sizeBefore, sizeAfter);

        // And
        long started = System.currentTimeMillis();
        try (DictionaryLabelStoreRocksDB modernStore = new DictionaryLabelStoreRocksDB(dbDir.toFile(), new StoreFmtByHash(HasherUtil.createXX128Hasher()))) {
            // Opening again should be almost immediate as migration has already happened
            Assertions.assertTrue(System.currentTimeMillis() - started < 10_000);
        }
    }
}
