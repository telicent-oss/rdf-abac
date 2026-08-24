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

import io.telicent.jena.abac.core.InitABAC;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.riot.web.HttpNames;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.util.function.Function;

import static io.telicent.jena.abac.fuseki.server.UserInfoEnrichmentFilter.ATTR_ABAC_USERNAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TestFusekiCoverageHelpers {

    @Test
    void initFusekiABAC_stopAndLevel() {
        InitFusekiABAC init = new InitFusekiABAC();
        init.stop();
        assertEquals(InitABAC.LEVEL + 1, init.level());
    }

    @Test
    void serverABAC_initAndUtilityConstructorAreCovered() throws Exception {
        Constructor<ServerABAC> ctor = ServerABAC.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());

        ServerABAC.init();
    }

    @Test
    void userForRequest_prefersEnrichedUsernameAndFallsBackToBearerHeader() {
        HttpAction action = mock(HttpAction.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(action.getRequest()).thenReturn(request);
        when(request.getAttribute(ATTR_ABAC_USERNAME)).thenReturn("alice");

        Function<HttpAction, String> lookup = ServerABAC.userForRequest();
        assertEquals("alice", lookup.apply(action));

        when(request.getAttribute(ATTR_ABAC_USERNAME)).thenReturn(null);
        when(request.getRemoteUser()).thenReturn(null);
        when(action.getRequestHeader(HttpNames.hAuthorization)).thenReturn("Bearer user:bob");
        assertEquals("bob", lookup.apply(action));

        when(action.getRequestHeader(HttpNames.hAuthorization)).thenReturn(null);
        assertNull(lookup.apply(action));
    }
}
