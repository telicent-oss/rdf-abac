package io.telicent.jena.abac.attributes;

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.AttributeValueSet;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Benchmarks attribute expression parsing and evaluation.
 * So we can see if changes to AE / AttributeExpr slow things down over time.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class AttributeExprBenchmark {

    /**
     * Different label expressions to test.
     * Going forward - add more, better labels
     */
    @Param({
            "role = 'editor'",
            "role = 'admin'",
            "region = 'UK'",
            "tag = 'news'"
    })
    public String exprText;

    /**
     * Number of attribute maps to evaluate against per benchmark invocation.
     */
    @Param({"128"})
    public int evalBatchSize;

    private AttributeExpr parsed;
    private List<Map<String, Object>> attributeSets;

    @Setup(Level.Trial)
    public void setup() {
        parsed = AE.parseExpr(exprText);

        attributeSets = new ArrayList<>(evalBatchSize);
        Random rnd = new Random(123);

        for (int i = 0; i < evalBatchSize; i++) {
            Map<String, Object> attrs = new HashMap<>();
            // Just some plausible attributes:
            attrs.put("role", switch (i % 4) {
                case 0 -> "viewer";
                case 1 -> "editor";
                case 2 -> "admin";
                default -> "guest";
            });
            attrs.put("region", switch (i % 3) {
                case 0 -> "UK";
                case 1 -> "US";
                default -> "EU";
            });
            attrs.put("clearance", rnd.nextInt(5));
            attrs.put("tag", switch (i % 5) {
                case 0 -> "sports";
                case 1 -> "news";
                case 2 -> "weather";
                case 3 -> "finance";
                default -> "travel";
            });

            attributeSets.add(attrs);
        }
    }

    @Benchmark
    public AttributeExpr parse_only(Blackhole bh) {
        AttributeExpr expr = AE.parseExpr(exprText);
        bh.consume(expr);
        return expr;
    }

    @Benchmark
    public void eval_only(Blackhole bh) {
        for (Map<String, Object> attrs : attributeSets) {
            boolean allowed = evaluate(parsed, attrs);
            bh.consume(allowed);
        }
    }

    private boolean evaluate(AttributeExpr expr, Map<String, Object> attrs) {
        List<AttributeValue> values = new ArrayList<>(attrs.size());
        for (Map.Entry<String, Object> e : attrs.entrySet()) {
            String name = e.getKey();
            Object v = e.getValue();
            ValueTerm term = (v instanceof Boolean b)
                    ? ValueTerm.value(b)
                    : ValueTerm.value(String.valueOf(v));
            values.add(AttributeValue.of(name, term));
        }
        AttributeValueSet avs = AttributeValueSet.of(values);
        ValueTerm result = AE.eval(expr, avs, null);
        return result.isBoolean() && result.getBoolean();
    }
}
