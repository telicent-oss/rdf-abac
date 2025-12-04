package io.telicent.jena.abac;

import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RdfValueLengthStatsByType {

    private static class Stats {
        long count = 0;
        long total = 0;
        Integer min = null;
        Integer max = null;

        void add(int len) {
            count++;
            total += len;
            if (min == null || len < min) {
                min = len;
            }
            if (max == null || len > max) {
                max = len;
            }
        }

        double avg() {
            return count == 0 ? 0.0 : (double) total / (double) count;
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: RdfValueLengthStatsByType <rootDir> [--bytes]");
            System.exit(1);
        }

        Path root = Paths.get(args[0]);
        boolean useBytes = Arrays.asList(args).contains("--bytes");

        if (!Files.isDirectory(root)) {
            System.err.println("Error: " + root + " is not a directory");
            System.exit(1);
        }

        List<Path> ttlFiles;
        try (Stream<Path> stream = Files.walk(root)) {
            ttlFiles = stream
                    .filter(p -> Files.isRegularFile(p)
                            && p.toString().toLowerCase().endsWith(".ttl"))
                    .collect(Collectors.toList());
        }

        if (ttlFiles.isEmpty()) {
            System.err.println("No .ttl files found under " + root);
            return;
        }

        System.err.println("Found " + ttlFiles.size() + " .ttl files. Processing...");

        Map<String, Stats> statsByType = new HashMap<>();

        for (Path ttl : ttlFiles) {
            System.err.println("  - " + ttl);
            processFile(ttl, statsByType, useBytes);
        }

        String unit = useBytes ? "bytes" : "chars";
        System.out.println("# rdf:type\tcount\tmin(" + unit + ")\tavg(" + unit + ")\tmax(" + unit + ")");

        // Sort by average length descending
        statsByType.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, Stats> e) -> e.getValue().avg())
                        .reversed())
                .forEach(e -> {
                    Stats s = e.getValue();
                    System.out.printf(
                            "%s\t%d\t%d\t%.1f\t%d%n",
                            e.getKey(),
                            s.count,
                            s.min == null ? 0 : s.min,
                            s.avg(),
                            s.max == null ? 0 : s.max
                    );
                });
    }

    private static void processFile(Path ttl,
                                    Map<String, Stats> statsByType,
                                    boolean useBytes) {
        Model model = ModelFactory.createDefaultModel();
        try {
            RDFDataMgr.read(model, ttl.toString());
        } catch (Exception ex) {
            System.err.println("[WARN] Failed to parse " + ttl + ": " + ex.getMessage());
            model.close();
            return;
        }

        try {
            // subject -> set of rdf:type IRIs (as Strings)
            Map<Resource, Set<String>> subjectTypes = new HashMap<>();

            StmtIterator typeIt = model.listStatements(null, RDF.type, (RDFNode) null);
            while (typeIt.hasNext()) {
                Statement st = typeIt.next();
                Resource subj = st.getSubject();
                RDFNode obj = st.getObject();
                if (!obj.isResource()) {
                    continue;
                }
                Resource typeRes = obj.asResource();
                String typeKey = typeRes.isURIResource()
                        ? typeRes.getURI()
                        : typeRes.toString(); // handle blank-node-ish types just in case

                subjectTypes
                        .computeIfAbsent(subj, k -> new HashSet<>())
                        .add(typeKey);
            }

            // Now walk all triples, attribute literal lengths to each subject's type(s)
            StmtIterator it = model.listStatements();
            while (it.hasNext()) {
                Statement st = it.next();
                RDFNode obj = st.getObject();
                if (!obj.isLiteral()) {
                    continue;
                }

                Resource subj = st.getSubject();
                Set<String> types = subjectTypes.get(subj);
                if (types == null || types.isEmpty()) {
                    continue; // subject has no rdf:type
                }

                Literal lit = obj.asLiteral();
                String lex = lit.getLexicalForm();
                int len = useBytes
                        ? lex.getBytes(StandardCharsets.UTF_8).length
                        : lex.length();

                for (String typeKey : types) {
                    Stats stats = statsByType.computeIfAbsent(typeKey, k -> new Stats());
                    stats.add(len);
                }
            }

        } finally {
            model.close();
        }
    }
}
