package io.telicent.jena.abac.labels.node;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.lang.BlankNodeAllocatorHash;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.riot.system.MapWithScope;

/**
 * Workaround class due to limitation in Jena.
 * Make use of provided id akin to BlankNodeAllocatorLabelEncoded but
 * leveraging BlankNodeAllocatorHash when creating new
 */
public class BlankNodeAllocatorLabelProvidedWithHash extends BlankNodeAllocatorHash implements MapWithScope.Allocator<String, Node, Node> {
    @Override
    public Node alloc(String label) {
        return NodeFactory.createBlankNode(NodeFmtLib.decodeBNodeLabel(label));
    }
    @Override
    public Node alloc(Node scope, String item) {
        return alloc(item);
    }
}