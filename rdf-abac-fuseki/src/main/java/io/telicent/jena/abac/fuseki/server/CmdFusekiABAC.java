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

import java.util.Set;

import io.telicent.jena.abac.fuseki.FMod_ABAC;
import jakarta.servlet.Filter;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.cmds.FusekiMain;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.fuseki.system.FusekiLogging;
import org.apache.jena.sys.JenaSystem;

/**
 * Run Jena Fuseki with rdf-abac available.
 */
public class CmdFusekiABAC {

    static {
        JenaSystem.init();
        FusekiLogging.setLogging();
    }

    public static void main(String ...args) {
        FusekiServer server = build(args).build();
        try {
            server.start();
            server.join();
        } catch (RuntimeException ex) {
            ex.printStackTrace();
        } finally { server.stop(); }
    }

    public static FusekiServer.Builder build(String ...args) {

        // Operations that need bearer auth.
        Set<Operation> bearerAuthOperations = Set.of(Operation.Query, Operation.GSP_R);

        // Specifics operation
        FMod_BearerAuthFilter bearerAuthFilter =
            new FMod_BearerAuthFilter(bearerAuthOperations,
                                      Authn::getUserFromToken64,
                                      BearerMode.REQUIRED);

        // ** FMod_BearerAuthFilter which only applies authn to query, not upload.
        // Must be after FMod_ABAC which can change plain operations into auth operations.

        FusekiModules modules = FusekiModules.create( new FMod_ABAC(), bearerAuthFilter);

        // If no FMod_BearerAuthFilter or want every request to be auth'ed.
        // Alternative - all requests
        // To apply the AuthBearerFilter servlet filter to all incoming requests.
        // Use OPTIONAL is for data upload, no user.
        Filter filter = new AuthBearerFilter2(Authn::getUserFromToken64, BearerMode.REQUIRED);

        FusekiServer.Builder builder =
            FusekiMain.builder(args)
                .fusekiModules(modules)
                // .addFilter("/*", filter)
                ;
        return builder;
    }
}
