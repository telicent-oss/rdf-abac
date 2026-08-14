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
import io.telicent.jena.abac.core.AttributesStore;
import jakarta.servlet.http.HttpServlet;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TestAttributeEvalServer {

    @Test
    void actionService_returnsServletAction() throws Exception {
        Constructor<AttributeEvalServer> ctor = AttributeEvalServer.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());

        AttributesStore store = new AttributesStore() {
            @Override public io.telicent.jena.abac.AttributeValueSet attributes(String user) { return null; }
            @Override public Set<String> users() { return Set.of(); }
            @Override public boolean hasHierarchy(Attribute attribute) { return false; }
            @Override public Hierarchy getHierarchy(Attribute attribute) { return null; }
        };
        HttpServlet servlet = AttributeEvalServer.actionService(store);
        assertNotNull(servlet);
        assertInstanceOf(org.apache.jena.fuseki.servlets.ServletAction.class, servlet);
    }
}
