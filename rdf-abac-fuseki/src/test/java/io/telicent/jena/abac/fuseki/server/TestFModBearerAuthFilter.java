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

package io.telicent.jena.abac.fuseki.server;

import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.auth.BearerMode;
import org.apache.jena.fuseki.server.DataAccessPoint;
import org.apache.jena.fuseki.server.DataAccessPointRegistry;
import org.apache.jena.rdf.model.ModelFactory;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TestFModBearerAuthFilter {

    @Test
    void configured_addsFilterForDeclaredPathspecs() {
        FMod_BearerAuthFilter module = new FMod_BearerAuthFilter(dap -> Set.of("/ds"),
                                                                 token -> token,
                                                                 BearerMode.REQUIRED);
        assertEquals("Bearer Authentication", module.name());

        FusekiServer.Builder builder = mock(FusekiServer.Builder.class);
        when(builder.addFilter(eq("/ds"), any())).thenReturn(builder);

        DataAccessPoint dap = mock(DataAccessPoint.class);
        DataAccessPointRegistry registry = mock(DataAccessPointRegistry.class);
        when(registry.accessPoints()).thenReturn(List.of(dap));

        module.prepare(builder, Set.of("/ds"), ModelFactory.createDefaultModel());
        module.configured(builder, registry, ModelFactory.createDefaultModel());

        verify(builder).addFilter(eq("/ds"), any());
    }
}
