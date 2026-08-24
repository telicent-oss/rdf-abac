/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac.evalserver;

import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.core.AttributeStoreCache;
import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.AttributesStoreRemote;
import org.apache.jena.cmd.CmdException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TestMainAttrEvalServer {

    @TempDir
    Path tempDir;

    @Test
    void configureStoreUrl_withRemoteStore_setsRemoteEndpoints() throws Exception {
        MainAttrEvalServer server = new MainAttrEvalServer(new String[0]);
        setField(server, "storeURL", "https://example.test/users/user1");

        invokePrivate(server, "configureStoreUrl");

        assertNull(field(server, "localAttributeStore"));
        assertEquals("https://example.test/users/user1", field(server, "lookupUserEndpoint"));
        assertEquals("https://example.test/users/user1", field(server, "lookupHierarchyEndpoint"));
    }

    @Test
    void configureStoreUrl_withFileStore_setsLocalStore() throws Exception {
        MainAttrEvalServer server = new MainAttrEvalServer(new String[0]);
        setField(server, "storeURL", "file:///tmp/attributes.ttl");

        invokePrivate(server, "configureStoreUrl");

        assertEquals("file:///tmp/attributes.ttl", field(server, "localAttributeStore"));
        assertNull(field(server, "lookupUserEndpoint"));
        assertNull(field(server, "lookupHierarchyEndpoint"));
    }

    @Test
    void configureStoreUrl_withRelativeOrMalformedStore_fails() throws Exception {
        MainAttrEvalServer relative = new MainAttrEvalServer(new String[0]);
        setField(relative, "storeURL", "relative/path.ttl");
        CmdException relativeEx = assertThrows(CmdException.class,
                                               () -> invokePrivate(relative, "configureStoreUrl"));
        assertTrue(relativeEx.getMessage().contains("Bad URI for attribute store"));

        MainAttrEvalServer malformed = new MainAttrEvalServer(new String[0]);
        setField(malformed, "storeURL", "http://bad host");
        CmdException malformedEx = assertThrows(CmdException.class,
                                                () -> invokePrivate(malformed, "configureStoreUrl"));
        assertTrue(malformedEx.getMessage().contains("Bad syntax in URI for attribute store"));
    }

    @Test
    void validateConfigurationSources_withoutStoreOrConfig_fails() {
        MainAttrEvalServer server = new MainAttrEvalServer(new String[0]);
        CmdException ex = assertThrows(CmdException.class,
                                       () -> invokePrivate(server, "validateConfigurationSources"));
        assertTrue(ex.getMessage().contains("Required: one of --attrstore and --config"));
    }

    @Test
    void validateConfigurationSources_withBothStoreAndConfig_fails() throws Exception {
        MainAttrEvalServer server = new MainAttrEvalServer(new String[0]);
        setField(server, "storeURL", "https://example.test/store");
        setField(server, "configFile", "/tmp/config.json");

        CmdException ex = assertThrows(CmdException.class,
                                       () -> invokePrivate(server, "validateConfigurationSources"));
        assertTrue(ex.getMessage().contains("Required: one of --attrstore and --config"));
    }

    @Test
    void loadConfigurationFile_populatesCacheSettings() throws Exception {
        Path config = tempDir.resolve("config.json");
        Files.writeString(config, """
                {
                  "userAttrStore": "https://example.test/users/{user}",
                  "hierarchyService": "https://example.test/hierarchies/{name}",
                  "cache": true,
                  "attributeCacheExpiryTime": "PT11S",
                  "hierarchyCacheExpiryTime": "PT7M",
                  "attributeCacheSize": 17,
                  "hierarchyCacheSize": 19
                }
                """);

        MainAttrEvalServer server = new MainAttrEvalServer(new String[0]);
        setField(server, "configFile", config.toString());

        invokePrivate(server, "loadConfigurationFile");

        assertEquals(Duration.ofSeconds(11), staticField("attributeCacheExpiry"));
        assertEquals(Duration.ofMinutes(7), staticField("hierarchyCacheExpiry"));
        assertEquals(17L, staticField("attributeCacheSize"));
        assertEquals(19L, staticField("hierarchyCacheSize"));
    }

    @Test
    void createStoreHelpers_coverLocalRemoteAndCachedStoreCreation() throws Exception {
        Method createRemote = MainAttrEvalServer.class
                .getDeclaredMethod("createRemoteAttributeStore", String.class, String.class);
        createRemote.setAccessible(true);

        Object remote = createRemote.invoke(null,
                                            "https://example.test/users/{user}",
                                            "https://example.test/hierarchies/{name}");
        assertInstanceOf(AttributesStoreRemote.class, remote);

        Method createLocal = MainAttrEvalServer.class
                .getDeclaredMethod("createLocalAttributeStore", String.class);
        createLocal.setAccessible(true);

        Path attrStore = Path.of(System.getProperty("basedir", "."))
                .resolve("../rdf-abac-fuseki/src/test/files/integration/attribute-store.ttl")
                .normalize();
        AttributesStore local = (AttributesStore) createLocal.invoke(null, attrStore.toString());
        assertNotNull(local.attributes("user1@email"));

        Method createCached = MainAttrEvalServer.class
                .getDeclaredMethod("createCachedAttributeStore", AttributesStore.class);
        createCached.setAccessible(true);

        setStaticField("attributeCacheExpiry", Duration.ofSeconds(1));
        setStaticField("hierarchyCacheExpiry", Duration.ofSeconds(2));
        setStaticField("attributeCacheSize", 3L);
        setStaticField("hierarchyCacheSize", 4L);

        AttributesStore store = new AttributesStore() {
            @Override public io.telicent.jena.abac.AttributeValueSet attributes(String user) { return null; }
            @Override public Set<String> users() { return Set.of(); }
            @Override public boolean hasHierarchy(Attribute attribute) { return false; }
            @Override public Hierarchy getHierarchy(Attribute attribute) { return null; }
        };
        Object cached = createCached.invoke(null, store);
        assertInstanceOf(AttributeStoreCache.class, cached);
    }

    @Test
    void parseDuration_usesDefaultForNullAndEmpty() throws Exception {
        Method parseDuration = MainAttrEvalServer.class
                .getDeclaredMethod("parseDuration", String.class, Duration.class);
        parseDuration.setAccessible(true);

        assertEquals(Duration.ofSeconds(1), parseDuration.invoke(null, null, Duration.ofSeconds(1)));
        assertEquals(Duration.ofMinutes(2), parseDuration.invoke(null, "", Duration.ofMinutes(2)));
    }

    @Test
    void parseDuration_rejectsBadSyntax() throws Exception {
        Method parseDuration = MainAttrEvalServer.class
                .getDeclaredMethod("parseDuration", String.class, Duration.class);
        parseDuration.setAccessible(true);

        InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                                                    () -> parseDuration.invoke(null, "not-a-duration", Duration.ofSeconds(1)));
        assertInstanceOf(CmdException.class, ex.getCause());
        assertTrue(ex.getCause().getMessage().contains("Bad syntax in config file duration"));
    }

    @Test
    void summaryAndCommandNameRemainUnset() throws Exception {
        MainAttrEvalServer server = new MainAttrEvalServer(new String[0]);
        Method summary = MainAttrEvalServer.class.getDeclaredMethod("getSummary");
        summary.setAccessible(true);
        Method commandName = MainAttrEvalServer.class.getDeclaredMethod("getCommandName");
        commandName.setAccessible(true);

        assertNull(summary.invoke(server));
        assertNull(commandName.invoke(server));
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = MainAttrEvalServer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = MainAttrEvalServer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object staticField(String name) throws Exception {
        Field field = MainAttrEvalServer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(null);
    }

    private static void setStaticField(String name, Object value) throws Exception {
        Field field = MainAttrEvalServer.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static void invokePrivate(Object target, String methodName) throws Exception {
        Method method = MainAttrEvalServer.class.getDeclaredMethod(methodName);
        method.setAccessible(true);
        try {
            method.invoke(target);
        } catch (InvocationTargetException ex) {
            if (ex.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw ex;
        }
    }
}
