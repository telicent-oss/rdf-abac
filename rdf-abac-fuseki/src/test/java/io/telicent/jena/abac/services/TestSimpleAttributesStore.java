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

package io.telicent.jena.abac.services;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.core.AttributesStore;
import jakarta.servlet.http.HttpServlet;
import org.apache.jena.fuseki.servlets.ServletAction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestSimpleAttributesStore {

    @Test
    void createLookupServlets_acceptNullLogger() {
        AttributesStore store = store();

        HttpServlet userServlet = SimpleAttributesStore.createLookupUserAttributeServlet(store, null);
        HttpServlet hierarchyServlet = SimpleAttributesStore.createLookupHierarchyServlet(store, null);

        assertInstanceOf(ServletAction.class, userServlet);
        assertInstanceOf(ServletAction.class, hierarchyServlet);
    }

    @Test
    void run_servesUserAndHierarchyLookups() throws IOException, InterruptedException {
        String base = SimpleAttributesStore.run(0, store());
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> user = get(client, base + "/users/lookup/alice");
        assertEquals(200, user.statusCode());
        assertTrue(user.body().contains("\"attributes\""));
        assertTrue(user.body().contains("role"));

        HttpResponse<String> missingUser = get(client, base + "/users/lookup/missing");
        assertEquals(404, missingUser.statusCode());
        assertTrue(missingUser.body().contains("User not found"));

        HttpResponse<String> hierarchy = get(client, base + "/hierarchies/lookup/clearance");
        assertEquals(200, hierarchy.statusCode());
        assertTrue(hierarchy.body().contains("\"tiers\""));
        assertTrue(hierarchy.body().contains("official"));

        HttpResponse<String> missingHierarchy = get(client, base + "/hierarchies/lookup/unknown");
        assertEquals(404, missingHierarchy.statusCode());
        assertTrue(missingHierarchy.body().contains("Hierarchy not found"));
    }

    private static AttributesStore store() {
        return new AttributesStore() {
            @Override public AttributeValueSet attributes(String user) {
                return "alice".equals(user) ? AttributeValueSet.of("role", "clearance") : null;
            }

            @Override public Set<String> users() {
                return Set.of("alice");
            }

            @Override public boolean hasHierarchy(Attribute attribute) {
                return "clearance".equals(attribute.name());
            }

            @Override public Hierarchy getHierarchy(Attribute attribute) {
                if ( ! "clearance".equals(attribute.name()) ) {
                    return null;
                }
                return Hierarchy.create(attribute, "official", "secret");
            }
        };
    }

    private static HttpResponse<String> get(HttpClient client, String uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(uri)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
