package io.telicent.jena.abac.labels.node;

import org.apache.jena.graph.Node;
import org.apache.jena.riot.system.MapWithScope;

import java.util.Map;

/**
 * Workaround class due to inability to access Policy classes within Jena.
 * (see https://github.com/apache/jena/blob/be5b3bbf6eb3d2704da60aefb0c39f14b0a30a40/jena-arq/src/main/java/org/apache/jena/riot/lang/LabelToNode.java#L142)
 */
public class EmptyScopePolicy implements MapWithScope.ScopePolicy<String, Node, Node> {
    @Override
    public Map<String, Node> getScope(Node scope) {
        return null;
    }
    @Override
    public void clear() {}
}