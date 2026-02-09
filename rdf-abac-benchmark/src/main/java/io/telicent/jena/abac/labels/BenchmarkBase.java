package io.telicent.jena.abac.labels;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

abstract class BenchmarkBase {

    private static final int MAX_LABELS_PER_TRIPLE = 8;
    private static final int LABEL_TEXT_LENGTH = 32;
    static final int SUBJECT_CARDINALITY = 10_000;
    static final int PREDICATE_CARDINALITY = 32;

    Random random;

    /**
     * Generate a reproducible SPO triple for index {@code i}.
     * The pattern ensures:
     *  - many distinct objects
     *  - repeated subjects/predicates (more realistic index behaviour)
     */
    Triple generateDataTriple(int i) {
        final int sIndex = i % SUBJECT_CARDINALITY;
        final int pIndex = i % PREDICATE_CARDINALITY;
        final Node s = NodeFactory.createURI("http://example.org/s/" + sIndex);
        final Node p = NodeFactory.createURI("http://example.org/p/" + pIndex);
        final Node o = NodeFactory.createLiteralString("o-" + i);
        return Triple.create(s, p, o);
    }

    /**
     * Generate a small set of random label strings.
     * Using Label.fromText() ensures the full label machinery is exercised
     * (validation, AttributeExpr parsing, etc.).
     */
    List<Label> generateRandomLabels() {
        final int numLabels = 1 + random.nextInt(MAX_LABELS_PER_TRIPLE);
        final List<Label> labels = new ArrayList<>(numLabels);
        for (int i = 0; i < numLabels; i++) {
            final String text = RandomStringUtils.insecure()
                    .nextAlphanumeric(LABEL_TEXT_LENGTH);
            labels.add(Label.fromText(text));
        }
        return labels;
    }
}
