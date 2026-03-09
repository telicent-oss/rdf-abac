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

import io.telicent.jena.abac.labels.Label;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFWrapper;
import org.apache.jena.sparql.core.Quad;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.jena.riot.out.NodeFmtLib.strNT;

/**
 * Stream to separate out the ABAC graphs (labels) into given graphs and pass the data through to the underlying
 * StreamRDF.
 * <p>
 * Discard such data if the collecting graph is null.
 */
public class StreamSplitter extends StreamRDFWrapper {

    protected final Graph labelsGraph;
    private final Set<String> warningsIssued = new HashSet<>();
    private final Label dataDftLabels;
    private final boolean useDftLabels;

    public StreamSplitter(StreamRDF data, Graph labelsGraph, Label dataDftLabels) {
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
        defaultLabels(pattern(triple));
    }

    private void defaultLabels(Quad quad) {
        // Add [ authz:pattern '...quad...' ; authz:label "..label.." ]
        defaultLabels(pattern(quad));
    }

    private void defaultLabels(Node patternNode) {
        Node x = NodeFactory.createBlankNode();
        Triple pattern = Triple.create(x, VocabAuthzLabels.pPattern, patternNode);
        Triple label = Triple.create(x, VocabAuthzLabels.pLabel, NodeFactory.createLiteralString(dataDftLabels.getText()));
        labelsGraph.add(pattern);
        labelsGraph.add(label);
    }

    private static Node pattern(Triple triple) {
        return pattern(triple.getSubject(), triple.getPredicate(), triple.getObject());
    }

    private static Node pattern(Quad quad) {
        if (Objects.equals(quad.getGraph(), Quad.defaultGraphIRI)) {
            return pattern(quad.asTriple());
        }
        return pattern(quad.getGraph(), quad.getSubject(), quad.getPredicate(), quad.getObject());
    }

    private static Node pattern(Node... nodes) {
        return NodeFactory.createLiteralString(Arrays.stream(nodes).map(StreamSplitter::obtainStringFromNode).collect(
                Collectors.joining(" ")));
    }

    /**
     * Obtain the string representation of given node. Note: Blank node's have already been processed so do not need
     * further encoding.
     *
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
        if (useDftLabels) {
            defaultLabels(triple);
        }
        super.triple(triple);
    }

    @Override
    public void quad(Quad quad) {
        Node gn = quad.getGraph();
        if (VocabAuthz.graphForLabels.equals(gn)) {
            // Triple in the labels graph.
            // Add to accumulator graph
            labelsGraph.add(quad.asTriple());
            return;
        }

        if (useDftLabels) {
            defaultLabels(quad);
        }

        // Check and warn if the named graph URI starts with the Authz vocab.
        if (gn.isURI() && gn.getURI().startsWith(VocabAuthz.getURI())) {
            String name = gn.getURI();
            if (!warningsIssued.contains(name)) {
                Log.warn(this, "Reserved name space used for named graph: " + gn.getURI());
                warningsIssued.add(name);
            }
        }
        // Currently, data in named graphs isn't applicable. Pass through.
        super.quad(quad);
    }

    /**
     * Obtain any warnings that have occurred during processing
     *
     * @return a set of the unique errors
     */
    Set<String> getWarningsIssued() {
        return warningsIssued;
    }
}
