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

import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;

import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.core.VocabAuthz;
import org.apache.jena.fuseki.server.Operation;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.apache.jena.riot.web.HttpNames;

import static io.telicent.jena.abac.fuseki.server.UserInfoEnrichmentFilter.ATTR_ABAC_USERNAME;

public class ServerABAC {

    /**
     * Provides constants relating to custom ABAC operations for Fuseki that this module implements
     */
    public static class Vocab {
        // These operations directly apply the ABAC-versions of the processors.
        // FMod_ABAC also replaces the processor for the usual "fuseki:query" etc. operations.

        /** Operation : authz:query. Force use of a ABAC version of the query operation. */
        public static final Operation operationQueryLabels = Operation.alloc(VocabAuthz.getURI()+"query", "query_ABAC", "Query with data labels");
        /** Operation : authz:gsp-r. Force use of a ABAC version of the GSP Read operation. */
        public static final Operation operationGSPRLabels  = Operation.alloc(VocabAuthz.getURI()+"gsp-r", "GSP-R_ABAC", "Graph Store Protocol (Read) with data labels");
        /** Operation : authz:upload. Intercept ABAC data change operations. */
        public static final Operation operationUploadABAC  = Operation.alloc(VocabAuthz.getURI()+"upload", "upload_ABAC", "Upload with data labels");
        /** Operation : authz:labels. Fetch the labels for a ABAC dataset. */
        public static final Operation operationGetLabels = Operation.alloc(VocabAuthz.getURI()+"labels", "labels_ABAC", "Download the ABAC labels");
    }

    public static void init() {}

    /**
     * Security-Label : The default label that applies to a data payload.
     */
    public static final String hSecurityLabel = SysABAC.hSecurityLabel;

    // "Authorization: Bearer: user:NAME"
    private static final Pattern authHeaderPattern = Pattern.compile("\\s*Bearer\\s+user:(\\S*)\s*");
    /**
     * Given a Http servlet request (in HttpAction), find the user.
     */
    public static Function<HttpAction, String> userForRequest() {
        return action -> {
            // 1) Preferred: from UserInfoEnrichmentFilter
            HttpServletRequest req = action.getRequest();
            Object u = req.getAttribute(ATTR_ABAC_USERNAME);
            if (u instanceof String s && !s.isBlank()) return s;

            // 2) Legacy fallback: "Bearer user:<name>" header
            String legacy = userFromHTTP(action);
            if (legacy != null) return legacy;

            return null;
        };
    }

    private static String userFromHTTP(HttpAction action) {
        // HTTP authentication
        String hUser = action.getRequest().getRemoteUser();
        if ( hUser != null )
            return hUser;

        String authHeader = action.getRequestHeader(HttpNames.hAuthorization);
        if ( authHeader == null || authHeader.isBlank() ) {
            return null;
            //ServletOps.errorBadRequest("No Authorization header");
        }
        // Format "Bearer user:...."
        // Anchored pattern
        // This will be replaced by JWT authentication and moved to a separate filter for request processing
        Matcher m = authHeaderPattern.matcher(authHeader);
        if ( ! m.matches() )
            ServletOps.errorBadRequest("Bad Authorization header");
        String auser = m.group(1);
        return auser;
    }
}
