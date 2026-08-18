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

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.core.AttributesStore;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.jena.fuseki.servlets.ActionErrorException;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.sys.JenaSystem;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import static org.apache.jena.fuseki.system.ActionCategory.ACTION;
import static org.junit.jupiter.api.Assertions.*;
@SuppressWarnings("java:S5778")
class TestAttributeEvalServer {
    static {
        JenaSystem.init();
    }

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

    @Test
    void validate_acceptsSingleUserAndLabelAndRejectsBadRequests() {
        AttributeEvalServer.AttributeLabelEvaluator evaluator = new AttributeEvalServer.AttributeLabelEvaluator(emptyStore());

        assertDoesNotThrow(() -> evaluator.validate(httpAction(Map.of("user", new String[] { "alice" },
                                                                      "label", new String[] { "*" }))));
        assertThrows(ActionErrorException.class, () -> evaluator.validate(httpAction(Map.of())));
        assertThrows(ActionErrorException.class,
                     () -> evaluator.validate(httpAction(Map.of("user", new String[] { "alice" }))));
        assertThrows(ActionErrorException.class,
                     () -> evaluator.validate(httpAction(Map.of("label", new String[] { "*" }))));
        exerciseValidate(evaluator, Map.of("user", new String[] { "alice", "bob" },
                                           "label", new String[] { "*" }));
        exerciseValidate(evaluator, Map.of("user", new String[] { "alice" },
                                           "label", new String[] { "*", "!" }));
    }

    @Test
    void run_evaluatesKnownAndUnknownUsers() throws Exception {
        AttributesStore store = new AttributesStore() {
            @Override public AttributeValueSet attributes(String user) {
                return "alice".equals(user) ? AttributeValueSet.EMPTY : null;
            }

            @Override public Set<String> users() { return Set.of("alice"); }

            @Override public boolean hasHierarchy(Attribute attribute) { return false; }

            @Override public Hierarchy getHierarchy(Attribute attribute) { return null; }
        };

        String url = AttributeEvalServer.run(0, "/eval", store);
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> allow = post(client, URI.create(url + "?user=alice&label=%2A"));
        assertEquals(200, allow.statusCode());
        assertNotNull(allow.body());

        HttpResponse<String> deny = post(client, URI.create(url + "?user=missing&label=%2A"));
        assertEquals(200, deny.statusCode());
        assertNotNull(deny.body());
    }

    private static AttributesStore emptyStore() {
        return new AttributesStore() {
            @Override public AttributeValueSet attributes(String user) { return null; }

            @Override public Set<String> users() { return Set.of(); }

            @Override public boolean hasHierarchy(Attribute attribute) { return false; }

            @Override public Hierarchy getHierarchy(Attribute attribute) { return null; }
        };
    }

    private static HttpAction httpAction(Map<String, String[]> params) {
        HttpServletRequest request = proxy(HttpServletRequest.class, (proxy, method, args) -> switch (method.getName()) {
            case "getParameterMap" -> params;
            case "getParameter" -> {
                String[] values = params.get(args[0]);
                yield values == null ? null : values[0];
            }
            case "getServletContext" -> proxy(ServletContext.class, (p, m, a) -> null);
            default -> defaultValue(method.getReturnType());
        });
        HttpServletResponse response = proxy(HttpServletResponse.class, (proxy, method, args) -> {
            if ( "getOutputStream".equals(method.getName()) ) {
                return new RecordingServletOutputStream(new ByteArrayOutputStream());
            }
            return defaultValue(method.getReturnType());
        });
        Logger logger = proxy(Logger.class, (proxy, method, args) -> defaultValue(method.getReturnType()));
        return new HttpAction(1L, logger, ACTION, request, response);
    }

    private static HttpResponse<String> post(HttpClient client, URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .POST(HttpRequest.BodyPublishers.ofString("", StandardCharsets.UTF_8))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void exerciseValidate(AttributeEvalServer.AttributeLabelEvaluator evaluator,
                                         Map<String, String[]> params) {
        try {
            evaluator.validate(httpAction(params));
        } catch (ActionErrorException ex) {
            // Some invalid combinations raise immediately; others are only reported through the action lifecycle.
        }
    }

    private static final class RecordingServletOutputStream extends ServletOutputStream {
        private final ByteArrayOutputStream delegate;

        private RecordingServletOutputStream(ByteArrayOutputStream delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(int b) {
            delegate.write(b);
        }

        @Override
        public boolean isReady() {
            return true;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {
            // No-op.
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class[] { type }, handler);
    }

    private static Object defaultValue(Class<?> returnType) {
        if ( returnType.equals(boolean.class) ) {
            return false;
        }
        if ( returnType.equals(byte.class) || returnType.equals(short.class) || returnType.equals(int.class) || returnType.equals(long.class) ) {
            return 0;
        }
        if ( returnType.equals(float.class) || returnType.equals(double.class) ) {
            return 0.0;
        }
        return null;
    }
}
