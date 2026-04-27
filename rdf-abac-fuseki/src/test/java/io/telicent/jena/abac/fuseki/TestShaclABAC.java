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
 * <p>
 * Verifies that when a user POSTs shapes to the SHACL endpoint, the data graph
 * validated is the ABAC-filtered view for that user — so the user only receives
 * validation errors for data they are authorised to see.
 * </p>
 * <p>
 * Test data: `:s :mustBeInt "not-an-integer"` is labeled {@code manager};
 * `:s :mustBeInt 100` is labeled {@code engineer}.  The shape asserts that all
 * {@code :mustBeInt} values must be {@code xsd:integer}.
 * </p>
 * <ul>
 *   <li>u1 (manager) — sees the non-integer value → validation reports a violation</li>
 *   <li>u2 (engineer) — sees only the valid integer value → validation conforms</li>
 * </ul>
 */
public class TestShaclABAC {

    static {
        FusekiLogging.setLogging();
    }

    private static final String DIR = "src/test/files/server/";

    // Data loaded before each test: two :mustBeInt values with different labels.
    private static final String SHACL_TEST_DATA = """
            Content-type: application/trig

            PREFIX : <http://example/>
            PREFIX authz: <http://telicent.io/security#>

            :s :mustBeInt "not-an-integer" .
            :s :mustBeInt 100 .

            GRAPH authz:labels {
                [ authz:pattern ':s :mustBeInt "not-an-integer"' ; authz:label "manager" ] .
                [ authz:pattern ':s :mustBeInt 100' ; authz:label "engineer" ] .
            }
            """;

    // Shape: all :mustBeInt values must be xsd:integer.
    private static final String INTEGER_SHAPE = """
            PREFIX sh:  <http://www.w3.org/ns/shacl#>
            PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
            PREFIX :    <http://example/>

            :MustBeIntegerShape
                a sh:NodeShape ;
                sh:targetSubjectsOf :mustBeInt ;
                sh:property [
                    sh:path     :mustBeInt ;
                    sh:datatype xsd:integer ;
                ] .
            """;

    private static final String SH_CONFORMS = "http://www.w3.org/ns/shacl#conforms";

    private FusekiServer server;
    private String shaclURL;
    private String uploadURL;

    @BeforeAll
    public static void beforeAll() {
        FusekiLogging.setLogging();
    }

    @BeforeEach
    public void setup() {
        FusekiModule fmod = new FMod_ABAC();
        FusekiModules mods = FusekiModules.create(List.of(fmod));
        server = FusekiServer.create()
                .port(0)
                .fusekiModules(mods)
                .parseConfigFile(FileOps.concatPaths(DIR, "config-server-shacl.ttl"))
                .build()
                .start();

        String base = "http://localhost:" + server.getPort() + "/ds";
        shaclURL  = base + "/shacl?default";
        uploadURL = base + "/upload";

        PlayLib.sendStringHTTP(uploadURL, SHACL_TEST_DATA);
    }

    @AfterEach
    public void teardown() {
        if (server != null)
            server.stop();
    }

    @Test
    public void givenNoAuthorizationHeader_whenShaclValidate_thenBadRequest() throws IOException, InterruptedException {
        HttpResponse<String> response = shaclPost(shaclURL, null, INTEGER_SHAPE);
        assertEquals(400, response.statusCode());
    }

    @Test
    public void givenManagerUser_whenShaclValidate_thenViolationReportedForInvisibleData()
            throws IOException, InterruptedException {
        // u1 has the manager attribute and can see :s :mustBeInt "not-an-integer".
        // "not-an-integer" is not an xsd:integer, so the shape must report a violation.
        HttpResponse<String> response = shaclPost(shaclURL, "u1", INTEGER_SHAPE);
        assertEquals(200, response.statusCode());
        assertFalse(shaclConforms(response.body()), "Expected SHACL violation for u1 (manager)");
    }

    @Test
    public void givenEngineerUser_whenShaclValidate_thenNoViolationForDataOutsideTheirView()
            throws IOException, InterruptedException {
        // u2 has the engineer attribute and can only see :s :mustBeInt 100.
        // 100 is a valid xsd:integer, so the shape must conform — even though the
        // manager-labeled "not-an-integer" value exists in the underlying dataset.
        HttpResponse<String> response = shaclPost(shaclURL, "u2", INTEGER_SHAPE);
        assertEquals(200, response.statusCode());
        assertTrue(shaclConforms(response.body()), "Expected SHACL to conform for u2 (engineer)");
    }

    // ---- helpers ----

    private static HttpResponse<String> shaclPost(String url, String user, String shapesTtl)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "text/turtle")
                .POST(HttpRequest.BodyPublishers.ofString(shapesTtl));
        if (user != null)
            builder.header("Authorization", "Bearer user:" + user);
        return HttpClient.newHttpClient().send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private static boolean shaclConforms(String responseTtl) {
        Model report = ModelFactory.createDefaultModel();
        RDFParser.fromString(responseTtl, Lang.TTL).parse(report);
        Property conforms = report.createProperty(SH_CONFORMS);
        return report.listStatements(null, conforms, (org.apache.jena.rdf.model.RDFNode) null)
                .nextStatement()
                .getObject()
                .asLiteral()
                .getBoolean();
    }
}