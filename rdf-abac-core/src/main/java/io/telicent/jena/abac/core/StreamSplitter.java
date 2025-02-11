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

package io.telicent.jena.abac.core;

import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.apache.jena.sparql.core.Quad;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.jena.riot.out.NodeFmtLib.strNT;

/**
 * Stream to separate out the ABAC graphs (labels) into given graphs
 * and pass the data through to the underlying StreamRDF.
 * <p>
 * Discard such data if the collecting graph is null.
 */
public class StreamSplitter extends StreamRDFWrapper {

    protected final Graph labelsGraph;
    private final Set<String> warningsIssued = new HashSet<>();
    private final List<String> dataDftLabels;
    private final boolean useDftLabels;

    public StreamSplitter(StreamRDF data, Graph labelsGraph, List<String> dataDftLabels) {
        super(data);
        this.labelsGraph = labelsGraph;
        this.dataDftLabels = dataDftLabels;
        this.useDftLabels = (dataDftLabels != null);
    }

    @Override
    public void prefix(String prefix, String uri) {
        super.prefix(prefix, uri);
        labelsGraph.getPrefixMapping().setNsPrefix(prefix, uri);
    }

    private void defaultLabels(Triple triple) {
        // Add  [ authz:pattern '...triple...' ;  authz:label "..label.." ] .
        Node x = NodeFactory.createBlankNode();
        for ( String label : dataDftLabels ) {
            Triple t1 = Triple.create(x, VocabAuthzLabels.pPattern, pattern(triple));
            Triple t2 = Triple.create(x, VocabAuthzLabels.pLabel, NodeFactory.createLiteralString(label));
            labelsGraph.add(t1);
            labelsGraph.add(t2);
        }
    }

    private static Node pattern(Triple triple) {
        String s = obtainStringFromNode(triple.getSubject())+" "+obtainStringFromNode(triple.getPredicate())+" "+obtainStringFromNode(triple.getObject());
        return NodeFactory.createLiteralString(s);
    }

    /**
     * Obtain the string representation of given node.
     * Note: Blank node's have already been processed so do not need
     * further encoding.
     * @param node to obtain string from
     * @return correct representation
     */
    private static String obtainStringFromNode(Node node) {
        if (node.isBlank()) {
            return node.toString();
        } else {
            return strNT(node);
        }
    }

    @Override
    public void triple(Triple triple) {
        if ( useDftLabels )
            defaultLabels(triple);
        super.triple(triple);
        return;
    }

    @Override
    public void quad(Quad quad) {
        if ( quad.isDefaultGraph() ) {
            // Data
            triple(quad.asTriple());
            return;
        }
        Node gn = quad.getGraph();
        if ( VocabAuthz.graphForLabels.equals(gn) ) {
            // Triple in the labels graph.
            // Add to accumulator graph
            if ( labelsGraph != null )
                labelsGraph.add(quad.asTriple());
            return;
        }

        // Check and warn if the named graph URI starts with the Authz vocab.
        if ( gn.isURI() && gn.getURI().startsWith(VocabAuthz.getURI()) ) {
            String name = gn.getURI();
            if ( ! warningsIssued.contains(name) )
                Log.warn(this, "Reserved name space used for named graph: "+gn.getURI());
            else
                warningsIssued.add(name);
        }
        // Currently, data in named graphs isn't applicable. Pass through.
        super.quad(quad);
    }
}
