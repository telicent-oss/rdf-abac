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

package io.telicent.jena.abac.fuseki;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import org.apache.jena.atlas.lib.FileOps;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.sys.FusekiModule;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ABAC-filtered SHACL validation endpoint.
 */
public class TestShaclABAC {

    static {
        FusekiLogging.setLogging();
    }

    private static final String DIR = "src/test/files/server/";

    private static final String SHACL_TEST_DATA = """
            Content-type: application/trig
            
            PREFIX authz: <http://telicent.io/security#>
            PREFIX ex:    <http://example.org/ns#>
            PREFIX foaf:  <http://xmlns.com/foaf/0.1/>
            
            ex:Alice
                a              foaf:Person ;
                foaf:givenName "Alice" ;
                foaf:knows     ex:Bob .
            
            ex:Bob
                a              foaf:Person ;
                foaf:givenName "Bob" ;
                foaf:knows     ex:Alice .
            
            GRAPH authz:labels {
                [ authz:pattern '<http://example.org/ns#Alice> <http://xmlns.com/foaf/0.1/knows> <http://example.org/ns#Bob>' ; authz:label "manager" ] .
                [ authz:pattern '<http://example.org/ns#Bob> <http://xmlns.com/foaf/0.1/knows> <http://example.org/ns#Alice>' ; authz:label "manager" ] .
            }
            """;

    private static final String PERSON_SHAPE_BAD = """
            PREFIX :     <http://example.org/ns#>
            PREFIX foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX schema: <http://schema.org/>
            PREFIX sh:   <http://www.w3.org/ns/shacl#>
            PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>
            
            :PersonShape
                a              sh:NodeShape ;
                sh:targetClass foaf:Person ;
                sh:property    [ sh:path     foaf:givenName ;
                                 sh:datatype xsd:string ; ] ;
                sh:property    [ sh:path  foaf:knows ;
                                 sh:class schema:Person ; ] .
            """;

    private static final String PERSON_SHAPE_GOOD = """
            PREFIX :     <http://example.org/ns#>
            PREFIX foaf: <http://xmlns.com/foaf/0.1/>
            PREFIX sh:   <http://www.w3.org/ns/shacl#>
            PREFIX xsd:  <http://www.w3.org/2001/XMLSchema#>
            
            :PersonShape
                a              sh:NodeShape ;
                sh:targetClass foaf:Person ;
                sh:property    [ sh:path     foaf:givenName ;
                                 sh:datatype xsd:string ; ] ;
                sh:property    [ sh:path  foaf:knows ;
                                 sh:class foaf:Person ; ] .
            """;

    private static final String SH_CONFORMS = "http://www.w3.org/ns/shacl#conforms";

    private FusekiServer server;
    private String shaclURL;

    @BeforeAll
    public static void beforeAll() {
        FusekiLogging.setLogging();
    }

    @BeforeEach
    public void setup() {
        final FusekiModule fmod = new FMod_ABAC();
        final FusekiModules mods = FusekiModules.create(List.of(fmod));
        server = FusekiServer.create()
                .port(0)
                .fusekiModules(mods)
                .parseConfigFile(FileOps.concatPaths(DIR, "config-server-shacl.ttl"))
                .build()
                .start();

        final String base = "http://localhost:" + server.getPort() + "/ds";
        shaclURL = base + "/shacl?default";
        final String uploadURL = base + "/upload";
        PlayLib.sendStringHTTP(uploadURL, SHACL_TEST_DATA);
    }

    @AfterEach
    public void teardown() {
        if (server != null)
            server.stop();
    }

    @Test
    public void givenNoAuthorizationHeader_whenShaclValidate_thenBadRequest() throws IOException, InterruptedException {
        final HttpResponse<String> response = shaclPost(shaclURL, null, PERSON_SHAPE_GOOD);
        assertEquals(400, response.statusCode());
    }

    @Test
    public void givenManagerUser_whenShaclValidate_thenViolationReportedForSecurityLabelledData()
            throws IOException, InterruptedException {
        // User u1 has the manager attribute and can see the foaf:knows relationship which according to the SHACL
        // should have a schema:Person as the object class. In the data foaf:knows points to a foaf:Person so the shape
        // must report a violation.
        final HttpResponse<String> response = shaclPost(shaclURL, "u1", PERSON_SHAPE_BAD);
        assertEquals(200, response.statusCode());
        assertFalse(shaclConforms(response.body()), "Expected SHACL violation for u1 (manager)");
    }

    @Test
    public void givenManagerUser_whenShaclValidate_thenNoViolationForSecurityLabelledData()
            throws IOException, InterruptedException {
        // User u1 has the manager attribute and can see the foaf:knows relationship which according to the SHACL
        // should have a foaf:Person as the object class. The data conforms to this shape so no violation should be
        // reported.
        final HttpResponse<String> response = shaclPost(shaclURL, "u1", PERSON_SHAPE_GOOD);
        assertEquals(200, response.statusCode());
        assertTrue(shaclConforms(response.body()), "Expected SHACL to conform for u1 (manager)");
    }

    @Test
    public void givenEngineerUser_whenShaclValidate_thenNoViolationForVisibleData()
            throws IOException, InterruptedException {
        // User u2 has the engineer attribute and cannot see the foaf:knows relationship. The visible data conforms to
        // the SHACL shape so no violation should be seen.
        final HttpResponse<String> response = shaclPost(shaclURL, "u2", PERSON_SHAPE_GOOD);
        assertEquals(200, response.statusCode());
        assertTrue(shaclConforms(response.body()), "Expected SHACL to conform for u2 (engineer)");
    }

    private static HttpResponse<String> shaclPost(String url, String user, String shaclShape)
            throws IOException, InterruptedException {
        final HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/turtle")
                .POST(HttpRequest.BodyPublishers.ofString(shaclShape));
        if (user != null) {
            builder.header("Authorization", "Bearer user:" + user);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private static boolean shaclConforms(String responseTtl) {
        final Model report = ModelFactory.createDefaultModel();
        RDFParser.fromString(responseTtl, Lang.TTL).parse(report);
        final Property conforms = report.createProperty(SH_CONFORMS);
        return report.listStatements(null, conforms, (org.apache.jena.rdf.model.RDFNode) null)
                .nextStatement()
                .getObject()
                .asLiteral()
                .getBoolean();
    }
}
