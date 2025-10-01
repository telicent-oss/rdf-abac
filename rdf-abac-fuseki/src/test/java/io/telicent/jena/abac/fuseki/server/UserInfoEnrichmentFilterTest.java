package io.telicent.jena.abac.fuseki.server;

import com.sun.net.httpserver.HttpServer;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.fuseki.main.auth.AuthBearerFilter;
import org.apache.jena.riot.web.HttpNames;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.telicent.jena.abac.fuseki.server.UserInfoEnrichmentFilter.ATTR_ABAC_USERNAME;
import static org.apache.jena.atlas.json.JSON.parseAny;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UserInfoEnrichmentFilterTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(null);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        System.clearProperty("ABAC_USERINFO_URL");
    }

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
        System.clearProperty("ABAC_USERINFO_URL");
    }

    @Nested
    public class doFilterTests {

        @Test
        @DisplayName("Happy Path /userinfo 200 with abac_attributes array")
        void doFilter_userinfo200_abacArray_setsUser_addsToCache_andContinues() throws Exception {
            // given
            server.createContext("/userinfo", ex -> {
                assertAuthorization(ex, "Bearer token123");
                setResponse(ex, 200, """
                        {
                          "preferred_username":"alice",
                          "attributes":{
                            "abac_attributes":["dept=eng","not-a-pair","clearance=TS"]
                          }
                        }""");
            });
            System.setProperty("ABAC_USERINFO_URL", baseUrl + "/userinfo");

            AuthBearerFilter legacy = mock(AuthBearerFilter.class);
            UserInfoEnrichmentFilter f = new UserInfoEnrichmentFilter(legacy);

            HttpServletRequest req = mock(HttpServletRequest.class);
            ServletResponse resp = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            when(req.getHeader(HttpNames.hAuthorization)).thenReturn("Bearer token123");

            // Mock static cache interactions
            try (MockedStatic<io.telicent.jena.abac.core.AttributesStoreAuthServer> st =
                         mockStatic(io.telicent.jena.abac.core.AttributesStoreAuthServer.class)) {
                st.when(() -> io.telicent.jena.abac.core.AttributesStoreAuthServer.getCachedUsername("token123"))
                        .thenReturn(null);

                // when
                f.doFilter(req, resp, chain);

                // then
                verify(req).setAttribute(ATTR_ABAC_USERNAME, "alice");
                verify(chain).doFilter(req, resp);
                verifyNoInteractions(legacy);
                st.verify(() -> io.telicent.jena.abac.core.AttributesStoreAuthServer.addIdAndUserName("token123", "alice"));
                st.verify(() -> io.telicent.jena.abac.core.AttributesStoreAuthServer.addUserNameAndAttributes(eq("alice"), any()), times(1));
            }
        }

        @Test
        @DisplayName("Happy Path /userinfo 200 with nested array")
        void doFilter_userinfo200_attributesObject_deepFlatten() throws Exception {
            // given
            server.createContext("/userinfo", ex -> {
                assertAuthorization(ex, "Bearer ZZZ");
                setResponse(ex, 200, """
                        {
                          "sub": "user123",
                          "attributes": {
                            "dept": "eng",
                            "clearance": ["TS","S"],
                            "meta": {
                              "oncall": true,
                              "level": 3,
                              "tags": ["x","y"],
                              "deep": { "inner":"v" }
                            }
                          }
                        }""");
            });
            System.setProperty("ABAC_USERINFO_URL", baseUrl + "/userinfo");

            AuthBearerFilter legacy = mock(AuthBearerFilter.class);
            UserInfoEnrichmentFilter f = new UserInfoEnrichmentFilter(legacy);

            HttpServletRequest req = mock(HttpServletRequest.class);
            ServletResponse resp = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            when(req.getHeader(HttpNames.hAuthorization)).thenReturn("BEARER ZZZ"); // case-insensitivity branch

            try (MockedStatic<io.telicent.jena.abac.core.AttributesStoreAuthServer> st =
                         mockStatic(io.telicent.jena.abac.core.AttributesStoreAuthServer.class)) {
                st.when(() -> io.telicent.jena.abac.core.AttributesStoreAuthServer.getCachedUsername("ZZZ"))
                        .thenReturn(null);

                // when
                f.doFilter(req, resp, chain);

                // then
                verify(req).setAttribute(ATTR_ABAC_USERNAME, "user123");
                verify(chain).doFilter(req, resp);
                verifyNoInteractions(legacy);

                // ensure attributes were added (we don't introspect the set deeply here)
                st.verify(() -> io.telicent.jena.abac.core.AttributesStoreAuthServer.addUserNameAndAttributes(eq("user123"), any()), times(1));
            }
        }

        @Test
        @DisplayName("Handle /userinfo 401 response - legacy path")
        void doFilter_userinfo401_fallsBackToLegacy() throws Exception {
            // given
            server.createContext("/userinfo", ex -> setResponse(ex, 401, ""));
            System.setProperty("ABAC_USERINFO_URL", baseUrl + "/userinfo");

            AuthBearerFilter legacy = mock(AuthBearerFilter.class);
            UserInfoEnrichmentFilter f = new UserInfoEnrichmentFilter(legacy);

            HttpServletRequest req = mock(HttpServletRequest.class);
            ServletResponse resp = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            when(req.getHeader(HttpNames.hAuthorization)).thenReturn("Bearer bad");

            // when
            f.doFilter(req, resp, chain);

            // then
            verify(legacy, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
            verify(chain, never()).doFilter(req, resp);
            verify(req, never()).setAttribute(eq(ATTR_ABAC_USERNAME), any());
        }

        @Test
        @DisplayName("Handle /userinfo exception - legacy path")
        void bearer_userinfoThrows_fallsBackToLegacy() throws Exception {
            // given
            System.setProperty("ABAC_USERINFO_URL", "http://rubbish/userinfo");

            AuthBearerFilter legacy = mock(AuthBearerFilter.class);
            UserInfoEnrichmentFilter f = new UserInfoEnrichmentFilter(legacy);

            HttpServletRequest req = mock(HttpServletRequest.class);
            ServletResponse resp = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            when(req.getHeader(HttpNames.hAuthorization)).thenReturn("Bearer xyz");

            // when
            f.doFilter(req, resp, chain);

            // then
            verify(legacy, times(1)).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
            verify(chain, never()).doFilter(req, resp);
        }

        @Test
        @DisplayName("Handle /userinfo 401 response - no legacy path")
        void doFilter_userinfo401_fallsBackToLegacy_notSet() throws Exception {
            // given
            server.createContext("/userinfo", ex -> setResponse(ex, 401, ""));
            System.setProperty("ABAC_USERINFO_URL", baseUrl + "/userinfo");

            AuthBearerFilter legacy = mock(AuthBearerFilter.class);
            UserInfoEnrichmentFilter f = new UserInfoEnrichmentFilter();

            HttpServletRequest req = mock(HttpServletRequest.class);
            ServletResponse resp = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            when(req.getHeader(HttpNames.hAuthorization)).thenReturn("Bearer bad");

            // when
            f.doFilter(req, resp, chain);

            // then
            verify(legacy, never()).doFilter(any(ServletRequest.class), any(ServletResponse.class), any(FilterChain.class));
            verify(chain, times(1)).doFilter(req, resp);
            verify(req, never()).setAttribute(eq(ATTR_ABAC_USERNAME), any());
        }

        @Test
        @DisplayName("Use cahced username if available.")
        void doFilter_cachedUsername() throws Exception {
            // given
            System.setProperty("ABAC_USERINFO_URL", baseUrl + "/userinfo"); // won't ever be called

            AuthBearerFilter legacy = mock(AuthBearerFilter.class);
            UserInfoEnrichmentFilter f = new UserInfoEnrichmentFilter(legacy);

            HttpServletRequest req = mock(HttpServletRequest.class);
            ServletResponse resp = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            when(req.getHeader(HttpNames.hAuthorization)).thenReturn("Bearer cachedtoken");

            try (MockedStatic<io.telicent.jena.abac.core.AttributesStoreAuthServer> st =
                         mockStatic(io.telicent.jena.abac.core.AttributesStoreAuthServer.class)) {
                st.when(() -> io.telicent.jena.abac.core.AttributesStoreAuthServer.getCachedUsername("cachedtoken"))
                        .thenReturn("bob");

                // when
                f.doFilter(req, resp, chain);

                // then
                verify(req).setAttribute(ATTR_ABAC_USERNAME, "bob");
                verify(chain).doFilter(req, resp);
                verifyNoInteractions(legacy);
            }
        }

        // --------- No header: just continues ---------

        @Test
        @DisplayName("If no header, continue")
        void doFilter_noBearerHeader_continues() throws Exception {
            // given
            System.setProperty("ABAC_USERINFO_URL", baseUrl + "/userinfo");

            AuthBearerFilter legacy = mock(AuthBearerFilter.class);
            UserInfoEnrichmentFilter f = new UserInfoEnrichmentFilter(legacy);

            HttpServletRequest req = mock(HttpServletRequest.class);
            ServletResponse resp = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            when(req.getHeader(HttpNames.hAuthorization)).thenReturn(null);

            // when
            f.doFilter(req, resp, chain);

            // then
            verify(chain).doFilter(req, resp);
            verifyNoInteractions(legacy);
        }

        @Test
        @DisplayName("Do not call if token blank")
        void blankToken_continues() throws Exception {
            // given
            System.setProperty("ABAC_USERINFO_URL", baseUrl + "/userinfo");

            AuthBearerFilter legacy = mock(AuthBearerFilter.class);
            UserInfoEnrichmentFilter f = new UserInfoEnrichmentFilter(legacy);

            HttpServletRequest req = mock(HttpServletRequest.class);
            ServletResponse resp = mock(ServletResponse.class);
            FilterChain chain = mock(FilterChain.class);

            when(req.getHeader(HttpNames.hAuthorization)).thenReturn("Bearer   ");

            // when
            f.doFilter(req, resp, chain);

            // then
            verify(chain).doFilter(req, resp);
            verifyNoInteractions(legacy);
        }
    }

    @Nested
    public class flattenFromAttributesObjectTest {

        @Test
        @DisplayName("Flatten map for attributes - happy path")
        void flattenFromAttributesObject_coversLegacyHelper() {
            // given
            String json = """
                    {
                      "dept":"eng",
                      "clearance":["TS","S"],
                      "flags":{"oncall":"true","mode":"blue"}
                    }
                    """;
            JsonValue parsedJSON = parseAny(json);

            // when
            java.util.List<String> pairs = UserInfoEnrichmentFilter.flattenFromAttributesObject(parsedJSON);

            // then
            assertNotNull(pairs);
            assertTrue(pairs.contains("dept=eng"));
            assertTrue(pairs.contains("clearance=TS"));
            assertTrue(pairs.contains("clearance=S"));
            assertTrue(pairs.contains("flags.oncall=true"));
            assertTrue(pairs.contains("flags.mode=blue"));
        }

        @Test
        @DisplayName("Flatten map for attributes - null")
        void flattenFromAttributesObject_null() {
            // given
            // wehn
            // then
            assertNull(UserInfoEnrichmentFilter.flattenFromAttributesObject(null));
        }

        @Test
        @DisplayName("Flatten map for attributes - not JSON object")
        void flattenFromAttributesObject_emptyString() {
            // given
            String json = "true";
            JsonValue notObjectJson = parseAny(json);
            // when, then
            assertNull(UserInfoEnrichmentFilter.flattenFromAttributesObject(notObjectJson));
        }

        @Test
        @DisplayName("Flatten map for attributes - emptyString")
        void flattenFromAttributesObject_nullInternal() {
            // given
            JsonObject object = new JsonObject();
            object.put("nullEntry", (JsonValue) null);

            java.util.List<String> pairs = UserInfoEnrichmentFilter.flattenFromAttributesObject(object);
            assertNotNull(pairs);
            assertTrue(pairs.isEmpty());
        }
    }

    @Nested
    public class flattenToAttributeValueSetTest {
        @Test
        @DisplayName("Null returns empty")
        void nullInput_returnsEmpty() {
            // given
            // when
            AttributeValueSet out = UserInfoEnrichmentFilter.flattenToAttributeValueSet(null);
            // then
            assertNotNull(out);
            assertTrue(out.isEmpty());
        }

        @Test
        @DisplayName("Test mix of entries")
        void array_mixedEntries_parsedAndInvalidIgnored() {
            // given
            JsonValue v = parseAny("""
                        ["dept=eng","not-a-pair","", "   ", "clearance=TS", 123, true, null]
                    """);
            // when
            AttributeValueSet out = UserInfoEnrichmentFilter.flattenToAttributeValueSet(v);

            // then
            assertNotNull(out);
            assertFalse(out.isEmpty());
            assertTrue(hasAttributeValue(out, "dept", "eng"));
            assertTrue(hasAttributeValue(out, "clearance", "TS"));

            // Ensure only the valid pairs were added
            // (size check is indirect since AttributeValueSet uses a multimap)
            assertEquals(1, out.get(Attribute.create("dept")).size());
            assertEquals(1, out.get(Attribute.create("clearance")).size());
            // invalid/blank/non-string entries should not create attributes
            assertFalse(out.hasAttribute(Attribute.create("not")));
        }

        // v is object: exercises processJsonObject path (scalars, arrays, nested objects)
        @Test
        @DisplayName("All supported types")
        void object_deepFlatten_allSupportedTypes() {
            // given
            JsonValue v = parseAny("""
                        {
                          "dept": "eng",
                          "clearance": ["TS","S"],
                          "meta": {
                            "oncall": true,
                            "level": 3
                          }
                        }
                    """);

            // when
            AttributeValueSet out = UserInfoEnrichmentFilter.flattenToAttributeValueSet(v);

            // then
            assertNotNull(out);
            assertFalse(out.isEmpty());

            // scalars
            assertTrue(hasAttributeValue(out, "dept", "eng"));
            // arrays
            assertTrue(hasAttributeValue(out, "clearance", "TS"));
            assertTrue(hasAttributeValue(out, "clearance", "S"));
            // nested object scalars
            assertTrue(hasAttributeValue(out, "meta.oncall", true));
            assertTrue(hasAttributeValue(out, "meta.level", "3"));
        }

        @Test
        @DisplayName("If not an object or array return empty")
        void scalarInput_returnsEmpty() {
            // given, when, then
            JsonValue vTrue = parseAny("true");
            AttributeValueSet outTrue = UserInfoEnrichmentFilter.flattenToAttributeValueSet(vTrue);
            assertTrue(outTrue.isEmpty());

            // given, when, then
            JsonValue vNum = parseAny("123");
            AttributeValueSet outNum = UserInfoEnrichmentFilter.flattenToAttributeValueSet(vNum);
            assertTrue(outNum.isEmpty());

            // given, when, then
            JsonValue vStr = parseAny("\"dept=eng\"");
            AttributeValueSet outStr = UserInfoEnrichmentFilter.flattenToAttributeValueSet(vStr);
            assertTrue(outStr.isEmpty());
        }
    }

    @Nested
    public class testTryFields {

        @Test
        @DisplayName("Return first match")
        void returnsFirstMatchImmediately() {
            // given
            JsonObject obj = parseAny("{\"a\":1, \"b\":\"x\"}").getAsObject();
            // when
            Optional<JsonValue> out = UserInfoEnrichmentFilter.tryFields(obj, List.of("a", "b"));
            // then
            assertTrue(out.isPresent());
            assertTrue(out.get().isNumber());
            assertEquals("1", out.get().getAsNumber().value().toString());
        }

        @Test
        @DisplayName("Skip missing")
        void skipsMissingAndFindsLaterField() {
            // given
            JsonObject obj = parseAny("{\"b\":\"x\"}").getAsObject();
            // when
            Optional<JsonValue> out = UserInfoEnrichmentFilter.tryFields(obj, List.of("a", "b"));
            // then
            assertTrue(out.isPresent());
            assertTrue(out.get().isString());
            assertEquals("x", out.get().getAsString().value());
        }

        @Test
        @DisplayName("Return empty if no fields")
        void returnsEmptyWhenNoFieldsPresent() {
            // given
            JsonObject obj = parseAny("{\"a\":1}").getAsObject();
            // when
            Optional<JsonValue> out = UserInfoEnrichmentFilter.tryFields(obj, List.of("z"));
            // then
            assertTrue(out.isEmpty());
        }

        @Test
        void returnsEmptyWhenNamesListIsEmpty() {
            //given
            JsonObject obj = parseAny("{\"a\":1}").getAsObject();
            // when
            Optional<JsonValue> out = UserInfoEnrichmentFilter.tryFields(obj, List.of());
            // then
            assertTrue(out.isEmpty());
        }

        @Test
        void returnsJsonNullIfFieldIsExplicitNull() {
            // given
            JsonObject obj = parseAny("{\"a\":null}").getAsObject();
            // when
            Optional<JsonValue> out = UserInfoEnrichmentFilter.tryFields(obj, List.of("a"));
            // then
            assertTrue(out.isPresent());
            assertTrue(out.get().isNull());
        }
    }

    @Nested
    class processNestedObjectTest {

        @Test
        void arrayBranch_includingNestedArrayAndObject() {
            // { "arr": ["A","B", true, 1, null, {"nested":"X"}, ["Y","Z"]] }
            JsonObject obj = parseAny("""
                        { "arr": ["A","B", true, 1, null, {"nested":"X"}, ["Y","Z"]] }
                    """).getAsObject();

            List<AttributeValue> out = new ArrayList<>();
            UserInfoEnrichmentFilter.processNestedObject("p", obj, out);

            Set<String> pairs = asPairs(out);
            // Scalars inside array
            assertTrue(pairs.contains("p.arr=A"));
            assertTrue(pairs.contains("p.arr=B"));
            assertTrue(pairs.contains("p.arr=true"));
            assertTrue(pairs.contains("p.arr=1"));
            // Nested object inside array -> prefix.field=value
            assertTrue(pairs.contains("p.arr.nested=X"));
            // Nested array inside array -> same base key
            assertTrue(pairs.contains("p.arr=Y"));
            assertTrue(pairs.contains("p.arr=Z"));
            // JSON null inside array should not add anything — nothing to assert; presence above is sufficient.
        }

        @Test
        void objectBranch_recursesIntoNestedObjects() {
            // { "o": { "inner":"val", "deep": { "x": 2 } } }
            JsonObject obj = parseAny("""
                        { "o": { "inner":"val", "deep": { "x": 2 } } }
                    """).getAsObject();

            List<AttributeValue> out = new ArrayList<>();
            UserInfoEnrichmentFilter.processNestedObject("p", obj, out);

            Set<String> pairs = asPairs(out);
            assertTrue(pairs.contains("p.o.inner=val"));
            assertTrue(pairs.contains("p.o.deep.x=2"));
        }

        @Test
        void scalarBranch_addsForStringBooleanNumber_ignoresJsonNull() {
            // { "s":"str", "b": true, "n": 42, "nullv": null }
            JsonObject obj = parseAny("""
                        { "s":"str", "b": true, "n": 42, "nullv": null }
                    """).getAsObject();

            List<AttributeValue> out = new ArrayList<>();
            UserInfoEnrichmentFilter.processNestedObject("p", obj, out);

            Set<String> pairs = asPairs(out);
            assertTrue(pairs.contains("p.s=str"));
            assertTrue(pairs.contains("p.b=true"));
            assertTrue(pairs.contains("p.n=42"));
            // Explicit JSON null is not a scalar (string/number/boolean) for our converter -> should not be present
            assertFalse(pairs.contains("p.nullv=")); // sanity check; exact "null" won't be added either
            assertFalse(pairs.stream().anyMatch(s -> s.startsWith("p.nullv=")));
        }
    }

    @Nested
    public class parseValueTest {
        @Test
        @DisplayName("parse invalid value")
        void test_invalidValue() {
            // given
            // when
            // then
            assertNull(UserInfoEnrichmentFilter.parseValue("ru!bbXish"));
        }
    }

    @Nested
    public class jsonScalarAsStringTest {
        @Test
        @DisplayName("")
        void test_nullValue() {
            // given
            // when
            // then
            assertNull(UserInfoEnrichmentFilter.jsonScalarAsString(null));
        }
    }

    private static void setResponse(com.sun.net.httpserver.HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes();
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void assertAuthorization(com.sun.net.httpserver.HttpExchange ex, String expected) {
        String got = ex.getRequestHeaders().getFirst("Authorization");
        if (got == null || !got.equals(expected)) {
            throw new AssertionError("Expected Authorization '" + expected + "', got '" + got + "'");
        }
    }

    private static boolean hasAttributeValue(AttributeValueSet set, String attribute, String value) {
        return set.get(Attribute.create(attribute)).contains(ValueTerm.value(value));
    }

    private static boolean hasAttributeValue(AttributeValueSet set, String attribute, Boolean value) {
        return set.get(Attribute.create(attribute)).contains(ValueTerm.value(value));
    }


    /**
     * Convert produced AttributeValues to "attr=value" strings for easy assertions.
     */
    private static Set<String> asPairs(List<AttributeValue> out) {
        return out.stream()
                .map(av -> av.attribute().name() + "=" + av.value().asString())
                .collect(Collectors.toSet());
    }

}
