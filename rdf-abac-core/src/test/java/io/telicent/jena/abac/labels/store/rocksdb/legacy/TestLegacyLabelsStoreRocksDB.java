package io.telicent.jena.abac.labels.store.rocksdb.legacy;

import io.telicent.jena.abac.labels.StoreFmtByString;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.rocksdb.ColumnFamilyHandle;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TestLegacyLabelsStoreRocksDB {

    private final RocksDBHelper mockHelper = mock(RocksDBHelper.class);
    private final ColumnFamilyHandle mockHandle = mock(ColumnFamilyHandle.class);
    private final RocksDB mockDB = mock(RocksDB.class);
    private File dbDir;

    @Test
    public void test_compact() throws Exception {
        when(mockHelper.removeFromColumnFamilyHandleList(eq(0))).thenReturn(mockHandle);
        when(mockHelper.openDB(anyString())).thenReturn(mockDB);
        dbDir = Files.createTempDirectory("tmpDirCompact").toFile();
        try(LegacyLabelsStoreRocksDB labelsStore = new LegacyLabelsStoreRocksDB(mockHelper, dbDir, new StoreFmtByString(), null)){
            labelsStore.compact();
            verify(mockDB, times(1)).compactRange(any(),any(),any());
        }
    }

    @Test
    public void test_compact_exception() throws Exception {
        when(mockHelper.removeFromColumnFamilyHandleList(eq(0))).thenReturn(mockHandle);
        when(mockHelper.openDB(anyString())).thenReturn(mockDB);
        doThrow(new RocksDBException("test")).when(mockDB).compactRange(any(),any(),any());
        dbDir = Files.createTempDirectory("tmpDirCompact").toFile();
        try(LegacyLabelsStoreRocksDB labelsStore = new LegacyLabelsStoreRocksDB(mockHelper, dbDir, new StoreFmtByString(), null)){
            assertThrows(RuntimeException.class, labelsStore::compact);
        }
    }

    @AfterEach
    public void tearDown() throws IOException {
        Files.deleteIfExists(dbDir.toPath());
    }
}
