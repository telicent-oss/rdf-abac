package io.telicent.jena.abac.labels.node;

import org.apache.jena.riot.lang.LabelToNode;

/**
 * Workaround class due to limitations in Jena
 * (see https://github.com/apache/jena/blob/be5b3bbf6eb3d2704da60aefb0c39f14b0a30a40/jena-arq/src/main/java/org/apache/jena/riot/lang/LabelToNode.java#L142)
 */
public class LabelToNodeGenerator {
    public static LabelToNode generate(){
        return new LabelToNode(new EmptyScopePolicy(), new BlankNodeAllocatorLabelProvidedWithHash());
    }
}