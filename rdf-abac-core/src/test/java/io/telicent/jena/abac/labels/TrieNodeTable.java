package io.telicent.jena.abac.labels;

import org.apache.jena.atlas.lib.Pair;
import org.apache.jena.graph.Node;
import org.apache.jena.tdb2.store.NodeId;
import org.apache.jena.tdb2.store.NodeIdFactory;
import org.apache.jena.tdb2.store.nodetable.NodeTable;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Trie-based node table for testing
 * <p>
 * More space-efficient than {@link NaiveNodeTable}, but a bit slower.
 * Prefer this for testing the loading of very large volumes of data to the label store.
 * <p>
 * Reverse lookup {@link NodeTable#getNodeForNodeId(NodeId)} (id to node) is not implemented.
 */
public class TrieNodeTable implements NodeTable {

    final AtomicLong idIndex = new AtomicLong();

    final TrieNodeMap idTable = new TrieNodeMap(idIndex::incrementAndGet);

    @Override
    public NodeId getAllocateNodeId(Node node) {
        return NodeIdFactory.createPtr(idTable.add(node));
    }

    @Override
    public NodeId getNodeIdForNode(Node node) {
        return null;
    }

    @Override
    public Node getNodeForNodeId(NodeId nodeId) {
        return null;
    }

    @Override
    public boolean containsNode(Node node) {
        return false;
    }

    @Override
    public boolean containsNodeId(NodeId nodeId) {
        return false;
    }

    @Override
    public List<NodeId> bulkNodeToNodeId(List<Node> list, boolean b) {
        return null;
    }

    @Override
    public List<Node> bulkNodeIdToNode(List<NodeId> list) {
        return null;
    }

    private static class WrapNodeMapIterator implements Iterator<Pair<NodeId, Node>> {

        WrapNodeMapIterator(Iterator<Pair<Node, Long>> nodeMapIterator) {
            this.nodeMapIterator = nodeMapIterator;
        }

        Iterator<Pair<Node, Long>> nodeMapIterator;

        @Override
        public boolean hasNext() {
            return nodeMapIterator.hasNext();
        }

        @Override
        public Pair<NodeId, Node> next() {
            var internal = nodeMapIterator.next();
            return Pair.create(NodeIdFactory.createPtr(internal.cdr()), internal.car());
        }
    }

    @Override
    public Iterator<Pair<NodeId, Node>> all() {

        return new WrapNodeMapIterator(idTable.iterator());
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public NodeTable wrapped() {
        return null;
    }

    @Override
    public void close() {

    }

    @Override
    public void sync() {

    }
}
