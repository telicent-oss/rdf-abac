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

import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.Track;
import io.telicent.jena.abac.fuseki.FMod_ABAC;
import jakarta.servlet.Filter;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.FusekiMainInfo;
import org.apache.jena.fuseki.main.FusekiServer;
import org.apache.jena.fuseki.main.auth.AuthBearerFilter;
import org.apache.jena.fuseki.main.auth.BearerMode;
import org.apache.jena.fuseki.main.cmds.FusekiMain;
import org.apache.jena.fuseki.main.sys.FusekiModules;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.sys.JenaSystem;

/**
 * Run Jena Fuseki with rdf-abac available.
 * <p>
 * <b>For development</b>
 * <p>
 * User is given by "Bearer user:NAME".
 */
public class CmdFusekiABAC {

    static {
        JenaSystem.init();
    }

    private static Symbol debugABAC = Symbol.create("abac:debug");

    public static void main(String ...args) {
        FusekiServer server = build(args).build();

        boolean bDebugABAC = Fuseki.getContext().isTrue(debugABAC);

        if ( bDebugABAC ) {
            FmtLog.info(SysABAC.SYSTEM_LOG, "Setting CxtABAC.systemTrace");
            CxtABAC.systemTrace(Track.TRACE);
        }

        try {
            FusekiMainInfo.logServer(Fuseki.serverLog, server, false);
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
        // FMod_BearerAuthFilter which only applies authn to query, not upload.
        FMod_BearerAuthFilter bearerAuthFilter =
            new FMod_BearerAuthFilter(bearerAuthOperations,
                                      Authn::getUserFromToken64,
                                      BearerMode.REQUIRED);


        // Must be after FMod_ABAC which can change plain operations into auth operations.
        FusekiModules modules = FusekiModules.create( new FMod_ABAC(), bearerAuthFilter);

        // Use this if either FMod_BearerAuthFilter is not being used, or every
        // request should be bearar-authed.
        //
        // Use OPTIONAL if data upload accepts requests without a bearer user
        // (e.g. it is controlled by a different authentication mechanism).
        Filter filter = new AuthBearerFilter(Authn::getUserFromToken64, BearerMode.REQUIRED);

        FusekiServer.Builder builder =
            FusekiMain.builder(args)
                .fusekiModules(modules)
                // .addFilter("/*", filter)
                ;
        return builder;
    }
}
