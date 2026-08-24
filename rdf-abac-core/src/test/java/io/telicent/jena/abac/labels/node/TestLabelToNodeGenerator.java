package io.telicent.jena.abac.labels.node;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.lang.LabelToNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TestLabelToNodeGenerator {

    @Test
    void test_labelToNode_create() {
        LabelToNode labelToNode = LabelToNodeGenerator.generate();
        Node result = labelToNode.create();
        assertNotNull(result);
        assertTrue(result.isBlank());
    }

    @Test
    void test_labelToNode_create_twice() {
        LabelToNode labelToNode = LabelToNodeGenerator.generate();
        Node result = labelToNode.create();
        assertNotNull(result);
        assertTrue(result.isBlank());
        Node result2 = labelToNode.create();
        assertNotNull(result2);
        assertTrue(result2.isBlank());
        assertNotEquals(result.toString(),result2.toString());
    }


    @Test
    void test_labelToNode_get_label() {
        LabelToNode labelToNode = LabelToNodeGenerator.generate();
        Node result = labelToNode.get(null, "label");
        assertNotNull(result);
        assertTrue(result.isBlank());
        assertEquals("_:label", result.toString());
    }

    @Test
    void test_labelToNode_get_encodedLabel() {
        LabelToNode labelToNode = LabelToNodeGenerator.generate();
        Node result = labelToNode.get(null, "B12345");
        assertNotNull(result);
        assertTrue(result.isBlank());
        assertEquals("_:12345", result.toString());
    }

}
