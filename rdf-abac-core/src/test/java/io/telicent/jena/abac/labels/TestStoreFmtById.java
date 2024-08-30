package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.labels.node.table.NaiveNodeTable;
import org.junit.jupiter.api.BeforeEach;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class TestStoreFmtById extends TestStoreFmt {

    @BeforeEach
    void setup() {

        byteBuffer = ByteBuffer.allocateDirect(LabelsStoreRocksDB.DEFAULT_BUFFER_CAPACITY).order(ByteOrder.LITTLE_ENDIAN);
        var nodeTable = new StoreFmtById(new NaiveNodeTable());
        encoder = nodeTable.createEncoder();
        parser = nodeTable.createParser();
    }
}
