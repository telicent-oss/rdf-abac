/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.tdb2.store.NodeId;
import org.apache.jena.tdb2.store.NodeIdFactory;
import org.apache.jena.tdb2.store.nodetable.NodeTable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
class TestStoreFmtByNodeId extends TestStoreFmt {

    private StoreFmtByNodeId format;

    @BeforeEach
    void setup() {
        NodeTable nodeTable = mock(NodeTable.class);
        Map<Node, Long> idsByNode = new HashMap<>();
        Map<Long, Node> nodesById = new HashMap<>();
        long[] nextId = {1L};

        when(nodeTable.getAllocateNodeId(any(Node.class))).thenAnswer(invocation -> {
            Node node = invocation.getArgument(0, Node.class);
            long id = idsByNode.computeIfAbsent(node, key -> {
                long allocated = nextId[0]++;
                nodesById.put(allocated, key);
                return allocated;
            });
            return NodeIdFactory.createPtr(id);
        });
        when(nodeTable.getNodeForNodeId(any(NodeId.class))).thenAnswer(invocation -> {
            NodeId nodeId = invocation.getArgument(0, NodeId.class);
            return nodesById.get(nodeId.getPtrLocation());
        });

        format = new StoreFmtByNodeId(nodeTable);
        byteBuffer = ByteBuffer.allocateDirect(LegacyLabelsStoreRocksDB.DEFAULT_BUFFER_CAPACITY)
                               .order(ByteOrder.LITTLE_ENDIAN);
        encoder = format.createEncoder();
        parser = format.createParser();
    }

    @Test
    void testToStringUsesSimpleClassName() {
        assertEquals("StoreFmtByNodeId", format.toString());
    }

    @Test
    void testParserRejectsWildcardNodeEncoding() {
        ByteBuffer buffer = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
        buffer.put((byte) (StoreFmt.NodeType.Any.ordinal() << 4));
        buffer.flip();
        assertThrows(LabelsException.class, () -> parser.parseSingleNode(buffer));
    }

    @Test
    void testUnimplementedQuadAndSingleLabelMethodsThrow() {
        assertThrows(RuntimeException.class, () -> encoder.formatLabel(byteBuffer, Label.fromText("x")));
        assertThrows(RuntimeException.class, () -> encoder.formatQuad(byteBuffer,
                                                                      SSE.parseNode(":g"),
                                                                      SSE.parseNode(":s"),
                                                                      SSE.parseNode(":p"),
                                                                      SSE.parseNode(":o")));
        assertThrows(RuntimeException.class, () -> parser.parseQuad(byteBuffer, new ArrayList<>()));
        assertThrows(RuntimeException.class, () -> parser.parseLabel(byteBuffer));
    }
}
