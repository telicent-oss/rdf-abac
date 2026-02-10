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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.telicent.jena.abac.fuseki.ServerABAC.Vocab;
import org.apache.jena.fuseki.server.OperationRegistry;
import org.apache.jena.fuseki.servlets.ActionService;
import org.apache.jena.riot.WebContent;

public class SysFusekiABAC {

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    public static void init() {
        boolean initialized = INITIALIZED.getAndSet(true);
         if ( initialized )
             return;
         // Load operations and handlers for Fuseki.
         // Registration is in SysABAC.
         // Use if authz:query and authz:upload needed.
         // Normally, FMod_ABAC replaces the processors for the regular query and upload operations,
         // fuseki:query, fuseki:upload (for ABAC_ChangeDispatch)

         ActionService queryLabelsProc = new ABAC_SPARQL_QueryDataset(ServerABAC.userForRequest());
         ActionService gspReadLabelsProc = new ABAC_GSP_R(ServerABAC.userForRequest());
         ActionService loaderLabelsProc = new ABAC_ChangeDispatch();
         ActionService labelsGetterProc = new ABAC_Labels();

         OperationRegistry operationRegistry = OperationRegistry.get();

         operationRegistry.register(Vocab.operationGSPRLabels, gspReadLabelsProc);
         operationRegistry.register(Vocab.operationUploadABAC, loaderLabelsProc);
         operationRegistry.register(Vocab.operationQueryLabels, queryLabelsProc);
         operationRegistry.register(Vocab.operationGetLabels, labelsGetterProc);

         /* Content type for which ABAC labelling applies. */
         Collection<String> dataContentTypes = langContentTypes();

         if ( dataContentTypes != null )
             // To allow multiple operations on the endpoint, need to register content types.
             dataContentTypes.forEach(ct-> operationRegistry.register(Vocab.operationUploadABAC, ct, loaderLabelsProc));
    }

    private static Collection<String> langContentTypes() {
        if ( true )
            // Only needed if the endpoint has multiple operations defined for it.
            // Easier - and safer - to have a special endpoint for labelled data ingestion.
            // Disable.
            return null;
        return List.of( WebContent.contentTypeTriG
                      , "text/trig"
                      , WebContent.contentTypeTurtle
                      , "application/turtle"
                      , WebContent.contentTypeNTriples
                      , "text/plain"
                      , WebContent.contentTypeNQuads
                      , WebContent.contentTypeJSONLD
                      , WebContent.contentTypeRDFProto
                      , WebContent.contentTypeRDFThrift
                      , WebContent.contentTypeRDFXML );

        // Some WebContent types were removed in Jena 6.0.0
        // These have been replaced here with text values as follows:
        // WebContent.contentTypeTriGAlt1 = "text/trig"
        // WebContent.contentTypeTurtleAlt1 = "application/turtle"
        // WebContent.contentTypeNTriplesAlt = "text/plain"

//        // Content-type for each language. Picks up junk.
//        Set<String> dataContentTypes = new HashSet<>();
//        RDFParserRegistry.registeredLangTriples().forEach(lang->dataContentTypes.add(lang.getHeaderString()));
//        RDFParserRegistry.registeredLangQuads().forEach(lang->dataContentTypes.add(lang.getHeaderString()));
//        System.err.println(dataContentTypes);
//        return dataContentTypes;
    }
}
