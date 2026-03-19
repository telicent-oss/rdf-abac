package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.StoreFmtByString;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;

import static io.telicent.jena.abac.rocks.modern.TestLabelStoreMigration.reportSizes;
import static io.telicent.jena.abac.rocks.modern.TestLabelStoreMigration.unpackZippedData;

/**
 * These tests are specifically designed to test behaviour around migrating pre-existing large label stores.  To run
 * these you will need a SCG backup archive containing a backup of {@code knowledge}.  The path to this ZIP archive
 * should be provided via the {@code large-test-data} System property.  If this is not provided then these tests will be
 * skipped.
 */
@SuppressWarnings("deprecation")
public class TestLargeLabelStoreMigration {

    private static Path backupDir;
    private Path dbDir;

    /**
     * Unpacks the test data once so each test can do a restore from this backup to start from a clean slate
     *
     * @throws IOException Thrown if the ZIP archive cannot be unpacked, or does not contain a backup of the
     *                     {@code knowledge} dataset
     */
    @BeforeAll
    public static void unpackTestData() throws IOException {
        String largeTestData = System.getProperty("large-test-data");
        Assumptions.assumeTrue(StringUtils.isNotBlank(largeTestData));

        backupDir = Files.createTempDirectory("rocks-backup");
        unpackZippedData(largeTestData, backupDir, "knowledge");
    }

    /**
     * Creates a temporary database directory and restores the previously unpacked backup into that directory
     *
     * @throws IOException Thrown if the database cannot be restored
     */
    @BeforeEach
    public void setup() throws IOException {
        this.dbDir = Files.createTempDirectory("rocks");
        restoreFromBackup(dbDir);
    }

    private static void restoreFromBackup(Path dbDir) {
        try (LegacyLabelsStoreRocksDB legacyStore = new LegacyLabelsStoreRocksDB(new RocksDBHelper(), dbDir.toFile(),
                                                                                 new StoreFmtByString(),
                                                                                 null)) {
            legacyStore.restore(backupDir.toFile().getAbsolutePath());
            Assertions.assertFalse(legacyStore.isEmpty());
        }
    }

    @Test
    public void givenLargeLegacyStore_whenOpeningWithModernStore_thenDataAutomaticallyMigrated_andOpeningAgainDoesNotRepeatMigration() throws
            IOException,
            RocksDBException {
        // Given
        long sizeBefore = FileUtils.sizeOfDirectory(dbDir.toFile());

        // When
        migrateToModernStore(dbDir.toFile());
        long sizeAfter = FileUtils.sizeOfDirectory(dbDir.toFile());
        reportSizes(sizeBefore, sizeAfter);

        // And
        verifyQuickOpen();
    }

    /**
     * Verifies that we can quickly open the database i.e. no long-running migration is needed before open can return
     *
     * @throws RocksDBException Thrown if the database cannot be opened
     * @throws IOException      Thrown if there is an IO problem
     */
    private void verifyQuickOpen() throws RocksDBException, IOException {
        long started = System.currentTimeMillis();
        try (DictionaryLabelStoreRocksDB modernStore = new DictionaryLabelStoreRocksDB(dbDir.toFile(),
                                                                                       new StoreFmtByHash(
                                                                                               HasherUtil.createXX128Hasher()))) {
            // Opening again should be almost immediate as migration has already happened
            Assertions.assertTrue(System.currentTimeMillis() - started < 10_000);
        }
    }

    @Test
    public void givenLargeLegacyStore_whenMigrationIsInterrupted_thenMigrationResumesOnReopen() throws
            InterruptedException, IOException {
        // Given
        long sizeBefore = FileUtils.sizeOfDirectory(dbDir.toFile());

        // When
        ProcessBuilder builder = new ProcessBuilder();
        builder.command("java", "-cp",
                        System.getProperty("java.class.path"),
                        this.getClass()
                            .getCanonicalName() + "$ExternalMigration",
                        dbDir.toFile().getAbsolutePath()
        );
        builder.inheritIO();
        Process externalMigration = builder.start();
        try {
            Thread.sleep(5_000);
            Assertions.assertTrue(externalMigration.isAlive());
            Thread.sleep(30_000);
        } finally {
            externalMigration.destroyForcibly();
            Assertions.assertNotEquals(0, externalMigration.waitFor());
        }

        // Then
        migrateToModernStore(dbDir.toFile());
        long sizeAfter = FileUtils.sizeOfDirectory(dbDir.toFile());
        reportSizes(sizeBefore, sizeAfter);
    }

    /**
     * Opens the given database location with the modern store implementation - {@link DictionaryLabelStoreRocksDB} -
     * which will trigger an automatic migration if it is a store in legacy format
     *
     * @param dbDir Database directory to open
     */
    static void migrateToModernStore(File dbDir) {
        try (DictionaryLabelStoreRocksDB modernStore = new DictionaryLabelStoreRocksDB(dbDir,
                                                                                       new StoreFmtByHash(
                                                                                               HasherUtil.createXX128Hasher()))) {
            // Then
            Assertions.assertFalse(modernStore.isEmpty());
            System.out.println();
            System.out.format("Unique Labels: %,d\n", modernStore.labelCount());
            System.out.format("Labelled Quads: %,d\n", modernStore.keyCount());
            System.out.println();
        } catch (IOException | RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    public static final class ExternalMigration {
        public static void main(String[] args) {
            migrateToModernStore(new File(args[0]));
        }
    }
}
