package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import org.junit.jupiter.api.BeforeEach;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TestStoreFmtByString extends TestStoreFmt {

    @BeforeEach
    void setup() {

        byteBuffer = ByteBuffer.allocateDirect(LegacyLabelsStoreRocksDB.DEFAULT_BUFFER_CAPACITY).order(ByteOrder.LITTLE_ENDIAN);
        encoder = new StoreFmtByString.Encoder();
        parser = new StoreFmtByString.Parser();
    }
}
