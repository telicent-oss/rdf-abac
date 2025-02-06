package io.telicent.jena.abac.labels.node;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.lang.LabelToNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestLabelToNodeGenerator {

    @Test
    public void test_labelToNode_create() {
        LabelToNodeGenerator generator = new LabelToNodeGenerator();
        LabelToNode labelToNode = generator.generate();
        Node result = labelToNode.create();
        assertNotNull(result);
        assertTrue(result.isBlank());
    }

    @Test
    public void test_labelToNode_get_label() {
        LabelToNodeGenerator generator = new LabelToNodeGenerator();
        LabelToNode labelToNode = generator.generate();
        Node result = labelToNode.get(null, "label");
        assertNotNull(result);
        assertTrue(result.isBlank());
        assertEquals("_:label", result.toString());
    }

    @Test
    public void test_labelToNode_get_encodedLabel() {
        LabelToNodeGenerator generator = new LabelToNodeGenerator();
        LabelToNode labelToNode = generator.generate();
        Node result = labelToNode.get(null, "B12345");
        assertNotNull(result);
        assertTrue(result.isBlank());
        assertEquals("_:12345", result.toString());
    }

}
