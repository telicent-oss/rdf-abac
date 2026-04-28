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

import org.apache.jena.atlas.web.MediaType;
import org.apache.jena.fuseki.DEF;
import org.apache.jena.fuseki.servlets.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.riot.web.HttpNames;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.web.HttpSC;

import java.util.Objects;
import java.util.function.Function;

import static org.apache.jena.fuseki.servlets.GraphTarget.determineTarget;

/**
 * SHACL validation service with ABAC filtering applied to the data graph.
 */
public class ABAC_SHACL_Validation extends SHACL_Validation implements ABAC_Processor {

    private final Function<HttpAction, String> getUser;

    public ABAC_SHACL_Validation(Function<HttpAction, String> getUser) {
        this.getUser = Objects.requireNonNull(getUser, "getUser is null");
    }

    @Override
    protected void doPost(HttpAction action) {
        final MediaType mediaType = ActionLib.contentNegotation(action, DEF.rdfOffer, DEF.acceptTurtle);
        final Lang lang = Objects.requireNonNullElse(
                RDFLanguages.contentTypeToLang(mediaType.getContentTypeStr()), RDFLanguages.TTL);
        final String targetNodeStr = action.getRequestParameter(HttpNames.paramTarget);
        action.beginRead();
        try {
            final DatasetGraph abacFilteredDsg = ABAC_Request.decideDataset(action, action.getActiveDSG(), getUser);
            final GraphTarget graphTarget = determineTarget(abacFilteredDsg, action);
            if (!graphTarget.exists()) {
                ServletOps.errorNotFound("No data graph: " + graphTarget.label());
            }
            final Graph data = graphTarget.graph();
            final Graph shapesGraph = ActionLib.readFromRequest(action, Lang.TTL);
            final Node targetNode = getTargetNode(targetNodeStr, data);
            final ValidationReport report = (targetNode == null)
                    ? ShaclValidator.get().validate(shapesGraph, data)
                    : ShaclValidator.get().validate(shapesGraph, data, targetNode);

            if (report.conforms())
                action.log.info("[{}] shacl: conforms", action.id);
            else
                action.log.info("[{}] shacl: {} validation error(s)", action.id, report.getEntries().size());

            action.setResponseStatus(HttpSC.OK_200);
            ActionLib.graphResponse(action, report.getGraph(), lang);
        } finally {
            action.endRead();
        }
    }

    private Node getTargetNode(String targetNodeStr, Graph data) {
        if (targetNodeStr != null) {
            final String x = data.getPrefixMapping().expandPrefix(targetNodeStr);
            return NodeFactory.createURI(x);
        } else {
            return null;
        }
    }

}
