/**
 * Copyright (C) Telicent Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.telicent.smart.cache.security.data.DataAccessAuthorizer;
import io.telicent.smart.cache.security.data.labels.SecurityLabels;
import io.telicent.smart.cache.security.data.labels.SecurityLabelsApplicator;
import io.telicent.smart.cache.security.data.plugins.DataSecurityPlugin;
import io.telicent.smart.cache.security.data.plugins.DataSecurityPluginLoader;
import io.telicent.smart.cache.security.data.requests.RequestContext;
import io.telicent.smart.caches.configuration.auth.UserInfo;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.apache.jena.sparql.graph.GraphFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Smoke test that the RDF-ABAC plugin image payload works when appended to the end of a host application classpath.
 * <p>
 * Run as a Java source file with a classpath of {@code data-security-core} plus its runtime dependencies, which stands in
 * for the host application, followed by {@code <plugin-dir>/*} containing the image JARs i.e.
 * </p>
 * <pre>
 * java -cp "core.jar:host-lib/*:plugin-dir/*" docker/PluginSmokeTest.java plugin-dir
 * </pre>
 * <p>
 * It checks that every class in the plugin JARs can be linked (so no runtime dependency was wrongly excluded from the
 * image), that the plugin is found via the {@code ServiceLoader} registration, and that the plugin APIs work: parser,
 * authorizer, applicator, Fuseki module, operations, lifecycle filters and dataset adapters.  Classes in the
 * third-party JARs are only loaded as far as those calls reach them.
 * Exits with a non-zero status, and an error message, on any failure.
 * </p>
 */
public class PluginSmokeTest {

    private static final String PLUGIN_CLASS = "io.telicent.smart.cache.security.data.plugins.rdf.abac.RdfAbacPlugin";
    private static final String PLUGIN_JAR_PREFIX = "data-security-plugin-rdf-abac";

    public static void main(String[] args) throws Exception {
        require(args.length == 1, "Usage: PluginSmokeTest <plugin-dir>");
        Path pluginDir = Path.of(args[0]);

        checkPluginClassesLink(pluginDir);

        DataSecurityPlugin plugin = DataSecurityPluginLoader.load();
        require(!DataSecurityPluginLoader.isFailSafeMode(), "Plugin loader is in fail-safe mode");
        require(PLUGIN_CLASS.equals(plugin.getClass().getName()),
                "Unexpected plugin loaded: " + plugin.getClass().getName());
        require(plugin.labelsParser() != null, "Plugin has no labels parser");
        require(plugin.labelsValidator() != null, "Plugin has no labels validator");

        checkAuthorizer(plugin);
        checkApplicator(plugin);
        checkLabelsModule(plugin);
        checkFusekiIntegration(plugin);
        checkDatasetAdapters(plugin);

        System.out.println("Plugin smoke test passed for " + plugin.getClass().getName());
    }

    /**
     * Loads every class in the plugin JARs and resolves its fields, methods and constructors, which forces the JVM to
     * link all the types they reference.  This detects any dependency missing from the host classpath plus image
     * payload, including on code paths that the remaining checks don't exercise.
     */
    private static void checkPluginClassesLink(Path pluginDir) throws IOException {
        List<String> failures = new ArrayList<>();
        int checked = 0;
        try (Stream<Path> files = Files.list(pluginDir)) {
            for (Path jar : files.filter(p -> p.getFileName().toString().startsWith(PLUGIN_JAR_PREFIX))
                                 .filter(p -> p.getFileName().toString().endsWith(".jar"))
                                 .sorted()
                                 .toList()) {
                try (ZipFile zip = new ZipFile(jar.toFile())) {
                    Enumeration<? extends ZipEntry> entries = zip.entries();
                    while (entries.hasMoreElements()) {
                        String name = entries.nextElement().getName();
                        if (!name.endsWith(".class") || name.endsWith("module-info.class")) {
                            continue;
                        }
                        String className = name.substring(0, name.length() - ".class".length()).replace('/', '.');
                        checked++;
                        try {
                            Class<?> clazz = Class.forName(className, false, PluginSmokeTest.class.getClassLoader());
                            clazz.getDeclaredFields();
                            clazz.getDeclaredMethods();
                            clazz.getDeclaredConstructors();
                        } catch (ClassNotFoundException | LinkageError e) {
                            failures.add(className + " -> " + e);
                        }
                    }
                }
            }
        }
        require(checked > 0, "No plugin classes found in " + pluginDir);
        require(failures.isEmpty(), "Plugin classes failed to link:\n  " + String.join("\n  ", failures));
        System.out.println("Linked " + checked + " plugin classes");
    }

    private static void checkAuthorizer(DataSecurityPlugin plugin) {
        RequestContext context = new TestRequestContext(
                UserInfo.builder().sub("smoke-test").attributes(Map.<String, Object>of("department", "engineering"))
                        .build());

        try (DataAccessAuthorizer authorizer = plugin.prepareAuthorizer(context)) {
            require(authorizer.canRead(parse(plugin, "department=engineering")),
                    "User should be able to read data matching their attributes");
            require(!authorizer.canRead(parse(plugin, "department=finance")),
                    "User should not be able to read data that doesn't match their attributes");
        }

        // No context available must fail-safe by denying everything
        try (DataAccessAuthorizer authorizer = plugin.prepareAuthorizer(null)) {
            require(!authorizer.canRead(parse(plugin, "department=engineering")),
                    "Missing request context must deny access");
        }
    }

    private static void checkApplicator(DataSecurityPlugin plugin) {
        byte[] defaultLabel = "department=engineering".getBytes(StandardCharsets.UTF_8);
        Triple triple = Triple.create(NodeFactory.createURI("http://example.org/s"),
                                      NodeFactory.createURI("http://example.org/p"),
                                      NodeFactory.createLiteralString("o"));

        // Dataset with an empty labels graph, if the plugin has one, exercises the labels store based applicator
        // otherwise this just exercises the default label applicator
        DatasetGraph dataset = DatasetGraphFactory.create();
        plugin.labelsGraphName().ifPresent(name -> dataset.addGraph(name, GraphFactory.createDefaultGraph()));

        try (SecurityLabelsApplicator applicator = plugin.prepareLabelsApplicator(defaultLabel, dataset)) {
            require(applicator.defaultLabel() != null, "Applicator has no default label");
            SecurityLabels<?> applied = applicator.labelForTriple(triple);
            require(applied != null, "Applicator didn't apply the default label to a triple");
            require(Arrays.equals(defaultLabel, applied.encoded()), "Applicator applied an unexpected label");
        }
    }

    /**
     * Instantiates the Fuseki module, which loads the {@code rdf-abac-fuseki} classes that none of the other checks
     * touch
     */
    private static void checkLabelsModule(DataSecurityPlugin plugin) {
        FusekiModule module = plugin.prepareLabelsModule().orElse(null);
        require(module != null, "Plugin did not supply a Fuseki labels module");
    }

    /**
     * Exercises the remaining plugin hooks that reference RDF-ABAC, Jena and Fuseki types: the Fuseki operations (which
     * reference {@code ServerABAC.Vocab}), label-to-node mapping, distribution lifecycle filters, and the (deprecated)
     * labels remover
     */
    @SuppressWarnings("removal")
    private static void checkFusekiIntegration(DataSecurityPlugin plugin) {
        require(!plugin.getReadOperations().isEmpty(), "Plugin declares no read operations");
        require(!plugin.getReadWriteOperations().isEmpty(), "Plugin declares no read-write operations");
        require(plugin.getReadOperations().stream().noneMatch(Objects::isNull), "Null read operation");
        require(plugin.getReadWriteOperations().stream().noneMatch(Objects::isNull), "Null read-write operation");
        require(plugin.prepareLabelToNode() != null, "Plugin has no label-to-node mapping");
        require(plugin.prepareDistributionLifecycleFilters().isPresent(), "Plugin has no distribution lifecycle filters");
        require(plugin.prepareLabelsRemover().isPresent(), "Plugin has no labels remover");
        require(plugin.labelsGraphName().isPresent(), "Plugin has no labels graph name");
    }

    /**
     * Checks the dataset related hooks behave sensibly for a dataset that isn't an ABAC dataset i.e. they report no
     * capability rather than failing
     */
    @SuppressWarnings("removal")
    private static void checkDatasetAdapters(DataSecurityPlugin plugin) {
        DatasetGraph plain = DatasetGraphFactory.create();
        require(plugin.prepareLabelledDataset(plain).isEmpty(), "Non-ABAC dataset must not be adapted as labelled");
        require(plugin.prepareLabelsBackup(plain).isEmpty(), "Non-ABAC dataset must have no labels backup");
        require(plugin.prepareLabelsRestore(plain).isEmpty(), "Non-ABAC dataset must have no labels restore");
        require(plugin.prepareLabelsCompact(plain).isEmpty(), "Non-ABAC dataset must have no labels compaction");
    }

    private static SecurityLabels<?> parse(DataSecurityPlugin plugin, String label) {
        return plugin.labelsParser().parseSecurityLabels(label.getBytes(StandardCharsets.UTF_8));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * A minimal request context, this only needs to supply the user info that the RDF-ABAC plugin uses
     */
    private record TestRequestContext(UserInfo userInfo) implements RequestContext {

        @Override
        public Jws<Claims> verifiedJwt() {
            return null;
        }

        @Override
        public String username() {
            return "smoke-test";
        }

        @Override
        public List<String> requestHeader(String header) {
            return List.of();
        }

        @Override
        public URI requestUri() {
            return null;
        }

        @Override
        public String requestPath() {
            return null;
        }

        @Override
        public String requestMethod() {
            return null;
        }
    }
}
