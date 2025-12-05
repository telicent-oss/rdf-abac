package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * End-to-end benchmark of the auth decision path:
 *   Triple -> labelsForTriples(triple) -> AttributeExpr eval -> decision
 * This gives you a single "ms per N decisions" metric that covers:
 *  - RocksDB label lookup
 *  - label parsing / AttributeExpr
 *  - evaluation against attribute maps
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class AuthDecisionBenchmark {

    @Param({"100000"})
    public int tripleCount;

    @Param({"1000000"})
    public int decisionsPerInvocation;

    private LabelsStoreRocksDB labelStore;
    private File dbDir;

    private Triple[] decisionTriples;
    private List<AttributeExpr>[] tripleExprs;
    private Map<String, Object> requestAttributes;

    private AttributeValueSet requestAvs;

    private Random random;

    @Setup(Level.Trial)
    @SuppressWarnings("unchecked")
    public void setup() throws IOException {
        random = new Random(123L);

        dbDir = Files.createTempDirectory("auth-jmh").toFile();
        dbDir.deleteOnExit();

        RocksDBHelper helper = new RocksDBHelper();
        StoreFmt storeFmt = new StoreFmtByString();
        labelStore = new LabelsStoreRocksDB(
                helper,
                dbDir,
                storeFmt,
                LabelsStoreRocksDB.LabelMode.Overwrite,
                null
        );

        decisionTriples = new Triple[decisionsPerInvocation];
        tripleExprs = (List<AttributeExpr>[]) new List<?>[tripleCount];

        for (int i = 0; i < tripleCount; i++) {
            Triple t = generateTriple(i);
            List<String> labelStrings = generateLabelStrings(i);
            List<Label> labels = new ArrayList<>(labelStrings.size());

            for (String s : labelStrings) {
                labels.add(Label.fromText(s));
            }
            labelStore.add(t, labels);

            List<AttributeExpr> exprs = new ArrayList<>(labelStrings.size());
            for (String s : labelStrings) {
                exprs.add(AE.parseExpr(s));
            }
            tripleExprs[i] = exprs;
        }

        // Decision workload: some hits, some repeated
        for (int i = 0; i < decisionsPerInvocation; i++) {
            decisionTriples[i] = generateRequestTriple(i);
        }

        requestAttributes = new HashMap<>();
        requestAttributes.put("role", "editor");
        requestAttributes.put("region", "UK");
        requestAttributes.put("clearance", 3);
        requestAttributes.put("tag", "news");

        this.requestAvs = toAttributeValueSet(requestAttributes);
    }

    @TearDown(Level.Trial)
    public void tearDown() {
        if (labelStore != null) {
            labelStore.close();
        }
        labelStore = null;
    }

    @Benchmark
    public void authz_decision(Blackhole bh) {
        for (int i = 0; i < decisionsPerInvocation; i++) {
            Triple t = decisionTriples[i];
            int idx = Math.floorMod(t.hashCode(), tripleCount);

            List<Label> labels = labelStore.labelsForTriples(t);

            boolean allowed = false;
            List<AttributeExpr> exprs = tripleExprs[idx];
            for (AttributeExpr expr : exprs) {
                boolean thisAllowed = evaluate(expr, requestAvs);
                if (thisAllowed) {
                    allowed = true;
                    break;
                }
            }
            bh.consume(labels);
            bh.consume(allowed);
        }
    }

    private Triple generateTriple(int i) {
        Node s = NodeFactory.createURI("http://example.org/resource/" + (i % 10000));
        Node p = NodeFactory.createURI("http://example.org/p/" + (i % 32));
        Node o = NodeFactory.createLiteralString("object-" + i);
        return Triple.create(s, p, o);
    }

    private Triple generateRequestTriple(int i) {
        // Mix: 75% exact hits, 25% "close but different object"
        int idx = i % tripleCount;
        Triple base = generateTriple(idx);
        if (i % 4 == 0) {
            // miss on object
            Node altO = NodeFactory.createLiteralString("object-miss-" + idx);
            return Triple.create(base.getSubject(), base.getPredicate(), altO);
        }
        return base;
    }

    private List<String> generateLabelStrings(int i) {
        List<String> labels = new ArrayList<>();
        switch (i % 4) {
            case 0 -> labels.add("role = 'editor'");
            case 1 -> labels.add("role = 'admin'");
            case 2 -> labels.add("region = 'UK'");
            case 3 -> labels.add("tag = 'news'");
        }
        return labels;
    }

    private boolean evaluate(AttributeExpr expr, AttributeValueSet avs) {
        ValueTerm result = AE.eval(expr, avs, null);
        return result.isBoolean() && result.getBoolean();
    }

    private static AttributeValueSet toAttributeValueSet(Map<String, Object> attrs) {
        List<AttributeValue> values = new ArrayList<>(attrs.size());
        attrs.forEach((name, v) -> {
            ValueTerm term;
            if (v instanceof Boolean b) {
                term = ValueTerm.value(b);
            } else {
                term = ValueTerm.value(String.valueOf(v));
            }
            values.add(AttributeValue.of(name, term));
        });
        return AttributeValueSet.of(values);
    }

    /**
     * For testing and debugging outside for JMH
     */
    public static void main(String[] args) throws IOException {
        AuthDecisionBenchmark benchmark = new AuthDecisionBenchmark();
        benchmark.tripleCount=1;
        benchmark.decisionsPerInvocation=10;
        benchmark.setup();
        Blackhole blackhole = new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
        benchmark.authz_decision(blackhole);
        benchmark.tearDown();
    }
}
