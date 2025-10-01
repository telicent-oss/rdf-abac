package io.telicent.jena.abac.core;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.Attribute;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

class TestAttributesStoreAuthServer {

    private final String uid = "id-" + System.nanoTime();
    private final String uname = "user-" + System.nanoTime();

    private Path tmpTurtle;

    private HttpClient mockHttpClient;
    private HttpResponse mockHttpResponse;

    @BeforeEach
    void setUp() throws IOException {
        tmpTurtle = Files.createTempFile("abac-hierarchies-", ".ttl");
        Files.writeString(tmpTurtle, "");
        assertTrue(Files.exists(tmpTurtle));
        // Sanity: can load empty graph
        Graph g = RDFDataMgr.loadGraph(tmpTurtle.toAbsolutePath().toString());
        assertNotNull(g);
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = Mockito.mock(HttpResponse.class);

    }

    @AfterEach
    void cleanup() throws IOException {
        if (tmpTurtle != null) {
            Files.deleteIfExists(tmpTurtle);
        }
    }

    @Nested
    @DisplayName("Static caches")
    class StaticCaches {

        @Test
        @DisplayName("addIdAndUserName + getCachedUsername")
        void idUserCache() {
            assertNull(AttributesStoreAuthServer.getCachedUsername(uid));
            AttributesStoreAuthServer.addIdAndUserName(uid, uname);
            assertEquals(uname, AttributesStoreAuthServer.getCachedUsername(uid));
        }

        @Test
        @DisplayName("addUserNameAndAttributes + attributes() + users()")
        void userAttributesCache() {
            AttributeValueSet avs = AttributeValueSet.of("role=admin", "clearance=TS");
            AttributesStoreAuthServer.addUserNameAndAttributes(uname, avs);

            AttributesStoreAuthServer store = new AttributesStoreAuthServer(null, null);
            assertEquals(avs, store.attributes(uname));

            Set<String> users = store.users();
            assertTrue(users.contains(uname));
        }
    }

    @Nested
    @DisplayName("Constructor branches & hierarchy calls")
    class ConstructorsAndHierarchy {

        @Test
        @DisplayName("Underlying Remote Hierarchy")
        @SuppressWarnings("unchecked")
        public void test_remoteHierarchy_() throws Exception {
            // given
            AttributesStoreAuthServer asr = new AttributesStoreAuthServer("http://localhost:8080/{name}", mockHttpClient);
            when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
            when(mockHttpResponse.statusCode()).thenReturn(200);
            String responseBody = """
                    {
                      "uuid" : "7fdbb786-37e4-3f6c-be99-c9fbb473aa2a" ,
                      "name" : "findHierarchy" ,
                      "tiers" : [
                          "dog" ,
                          "baby" ,
                          "junior" ,
                          "middle" ,
                          "eldest" ,
                          "matriarch"
                        ] ,
                      "levels" : [
                          "dog" ,
                          "baby" ,
                          "junior" ,
                          "middle" ,
                          "eldest" ,
                          "matriarch"
                        ]
                    }
                    """;
            InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
            when(mockHttpResponse.body()).thenReturn(testStream);
            // when, then
            assertNull(asr.attributes("missingUser"));
            assertTrue(asr.users().isEmpty());
            assertNotNull(asr.getHierarchy(Attribute.create("findHierarchy")));
        }


        @Test
        @DisplayName("Local file branch: empty TTL -> no hierarchies")
        void localFileBranch_noHierarchy() {
            AttributesStoreAuthServer store = new AttributesStoreAuthServer(tmpTurtle.toAbsolutePath().toString());
            assertNull(store.getHierarchy(Attribute.create("clearance")));
            assertFalse(store.hasHierarchy(Attribute.create("clearance")));
        }

        @Test
        @DisplayName("Remote URI branch (FileUtils.isURI true): constructor path covered")
        void remoteUriBranch_constructorCovered() {
            // http://... is recognized as a URI; we just exercise constructor branch.
            AttributesStoreAuthServer store = new AttributesStoreAuthServer("http://auth.example/hierarchy/{attr}");
            assertNotNull(store); // do not call getHierarchy() to avoid network in AttributesStoreRemote
        }

        @Test
        @DisplayName("Null hierarchy URL")
        void noHierarchyBranch() {
            AttributesStoreAuthServer store1 = new AttributesStoreAuthServer((String) null);
            assertNull(store1.getHierarchy(Attribute.create("x")));
            assertFalse(store1.hasHierarchy(Attribute.create("x")));
        }

        @Test
        @DisplayName("Blank hierarchy URL")
        void emptyHierarchyBranch() {
            AttributesStoreAuthServer store2 = new AttributesStoreAuthServer("");
            assertNull(store2.getHierarchy(Attribute.create("y")));
            assertFalse(store2.hasHierarchy(Attribute.create("y")));
        }

    }
}
