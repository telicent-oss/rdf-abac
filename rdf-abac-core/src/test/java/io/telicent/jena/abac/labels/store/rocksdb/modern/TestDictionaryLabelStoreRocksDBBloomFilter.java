package io.telicent.jena.abac.labels.store.rocksdb.modern;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The label store opens without Smart Cache Storage's bloom filter unless
 * {@link DictionaryLabelStoreRocksDB#BLOOM_FILTER_PROPERTY} is set, and stores written with the filter still read back
 * correctly without it.
 */
@SuppressWarnings("java:S100")
class TestDictionaryLabelStoreRocksDBBloomFilter {

    private static final int QUADS = 2_000;
    private static final List<Label> LABELS =
            List.of(Label.fromText("public"), Label.fromText("restricted"), Label.fromText("secret"));
    /** Name of the meta block RocksDB writes for a whole-file (full) filter */
    private static final byte[] FULL_FILTER_BLOCK = "fullfilter".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    Path tempDir;

    @AfterEach
    void clearProperty() {
        System.clearProperty(DictionaryLabelStoreRocksDB.BLOOM_FILTER_PROPERTY);
    }

    @Test
    void tableConfig_hasNoFilterByDefault() throws Exception {
        try (DictionaryLabelStoreRocksDB store = createStore(tempDir.resolve("default").toFile())) {
            assertNull(store.createBlockBasedTableConfig().filterPolicy());
        }
    }

    @Test
    void tableConfig_keepsFilterWhenPropertySet() throws Exception {
        System.setProperty(DictionaryLabelStoreRocksDB.BLOOM_FILTER_PROPERTY, "true");
        try (DictionaryLabelStoreRocksDB store = createStore(tempDir.resolve("opt-in").toFile())) {
            assertNotNull(store.createBlockBasedTableConfig().filterPolicy());
        }
    }

    @Test
    void newFiles_haveNoFilterBlock() throws Exception {
        File dir = tempDir.resolve("new-files").toFile();
        try (DictionaryLabelStoreRocksDB store = createStore(dir)) {
            addLabels(store, 0, QUADS);
            store.compact();
        }
        List<Path> ssts = sstFiles(dir);
        assertFalse(ssts.isEmpty(), "Expected compaction to write SST files");
        for (Path sst : ssts) {
            assertFalse(contains(sst, FULL_FILTER_BLOCK), sst + " should not have a filter block");
        }
    }

    @Test
    void storeWrittenWithFilter_readsBackWithout() throws Exception {
        File dir = tempDir.resolve("upgrade").toFile();

        // Written as by earlier releases, with the bloom filter
        System.setProperty(DictionaryLabelStoreRocksDB.BLOOM_FILTER_PROPERTY, "true");
        try (DictionaryLabelStoreRocksDB store = createStore(dir)) {
            addLabels(store, 0, QUADS);
            store.compact();
        }
        assertTrue(sstFiles(dir).stream().anyMatch(sst -> contains(sst, FULL_FILTER_BLOCK)),
                   "Expected the store to have been written with filter blocks");

        // Reopened by this release, without the filter
        System.clearProperty(DictionaryLabelStoreRocksDB.BLOOM_FILTER_PROPERTY);
        try (DictionaryLabelStoreRocksDB store = createStore(dir)) {
            assertLabels(store, 0, QUADS);
            assertNull(store.labelForQuad(quad(QUADS + 1)), "A quad with no stored label must still miss");

            addLabels(store, QUADS, 2 * QUADS);
            store.compact();
            assertLabels(store, 0, 2 * QUADS);
        }
    }

    private static DictionaryLabelStoreRocksDB createStore(File dir) throws Exception {
        return new DictionaryLabelStoreRocksDB(dir, new StoreFmtByHash(HasherUtil.createXX128Hasher()));
    }

    private static Quad quad(int i) {
        return Quad.create(NodeFactory.createURI("http://example/g" + (i % 7)),
                           NodeFactory.createURI("http://example/s" + i),
                           NodeFactory.createURI("http://example/p"),
                           NodeFactory.createLiteralString("o" + i));
    }

    private static void addLabels(DictionaryLabelStoreRocksDB store, int from, int to) {
        Transactional txn = store.getTransactional();
        txn.begin(ReadWrite.WRITE);
        try {
            for (int i = from; i < to; i++) {
                store.add(quad(i), LABELS.get(i % LABELS.size()));
            }
            txn.commit();
        } finally {
            txn.end();
        }
    }

    private static void assertLabels(DictionaryLabelStoreRocksDB store, int from, int to) {
        for (int i = from; i < to; i++) {
            assertEquals(LABELS.get(i % LABELS.size()), store.labelForQuad(quad(i)), "Label for quad " + i);
        }
    }

    private static List<Path> sstFiles(File dir) throws IOException {
        try (Stream<Path> files = Files.list(dir.toPath())) {
            return files.filter(p -> p.getFileName().toString().endsWith(".sst")).toList();
        }
    }

    private static boolean contains(Path file, byte[] needle) {
        try {
            byte[] data = Files.readAllBytes(file);
            outer:
            for (int i = 0; i <= data.length - needle.length; i++) {
                for (int j = 0; j < needle.length; j++) {
                    if (data[i + j] != needle[j]) {
                        continue outer;
                    }
                }
                return true;
            }
            return false;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
