package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import io.telicent.jena.abac.labels.StoreFmtByString;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.*;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static io.telicent.jena.abac.AbstractTestLabelsStore.HUGE_STRING;
import static io.telicent.jena.abac.core.VocabAuthzDataset.pLabelsStoreByteBufferSize;
import static org.apache.jena.sparql.sse.SSE.parseTriple;
import static org.junit.jupiter.api.Assertions.*;

public class TestLabelsStoreRocksDBByteBufferConfig {
    File dbDirectory;
    static Model model = ModelFactory.createDefaultModel();
    final static Triple HUGE_TRIPLE = parseTriple("(:s :p '" + HUGE_STRING + "')");

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
    public void test_happyConfig_property() {
        Resource r = model.createResource("test_happyConfig_property");
        r.addProperty(pLabelsStoreByteBufferSize, "800000");
        try {
            LabelsStore store = Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.Overwrite, r, new StoreFmtByString());
            assertNotNull(store);
            store.add(HUGE_TRIPLE, "hugeLabel");
            List<String> x = store.labelsForTriples(HUGE_TRIPLE);
            assertEquals(List.of("hugeLabel"), x);
        } catch (RocksDBException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @Test
    public void test_badConfig_negative() {
        Resource r = model.createResource("test_badConfig_negative");
        r.addProperty(pLabelsStoreByteBufferSize, "-1");
        assertThrows(RuntimeException.class, () -> Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.Overwrite, r, new StoreFmtByString()));
    }

    @Test
    public void test_badConfig_string() {
        Resource r = model.createResource("test_badConfig_string");
        r.addProperty(pLabelsStoreByteBufferSize, "Wrong");
        assertThrows(RuntimeException.class, () -> Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.Overwrite, r, new StoreFmtByString()));
    }

    @Test
    public void test_badConfig_OverMaxInt() {
        Resource r = model.createResource("test_badConfig_OverMaxInt");
        long maxIntValue = Integer.MAX_VALUE;
        r.addLiteral(pLabelsStoreByteBufferSize, ++maxIntValue);
        assertThrows(RuntimeException.class, () -> Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.Overwrite, r, new StoreFmtByString()));
    }

    @Test
    public void test_happyConfig_long() {
        Resource r = model.createResource("test_happyConfig_long");
        r.addLiteral(pLabelsStoreByteBufferSize, 700000L);
        try {
            LabelsStore store = Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.Overwrite, r, new StoreFmtByString());
            assertNotNull(store);
            store.add(HUGE_TRIPLE, "hugeLabel");
            List<String> x = store.labelsForTriples(HUGE_TRIPLE);
            assertEquals(List.of("hugeLabel"), x);
        } catch (RocksDBException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @Test
    public void test_exceptionThrownIfBufferTooSmall() {
        Resource r = model.createResource("test_exceptionThrownIfBufferTooSmall");
        r.addProperty(pLabelsStoreByteBufferSize, "6500");
        try {
            LabelsStore store = Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.Overwrite, r, new StoreFmtByString());
            assertNotNull(store);
            assertThrows(RuntimeException.class, () -> store.add(HUGE_TRIPLE, "hugeLabel"));
        } catch (RocksDBException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @Test
    public void test_justLargeEnoughBuffer() {
        Resource r = model.createResource("test_justLargeEnoughBuffer");
        r.addProperty(pLabelsStoreByteBufferSize, "6600");
        try {
            LabelsStore store = Labels.createLabelsStoreRocksDB(dbDirectory, LabelsStoreRocksDB.LabelMode.Overwrite, r, new StoreFmtByString());
            assertNotNull(store);
            store.add(HUGE_TRIPLE, "hugeLabel");
            List<String> x = store.labelsForTriples(HUGE_TRIPLE);
            assertEquals(List.of("hugeLabel"), x);
        } catch (RocksDBException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }
}