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

package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.attributes.AttributeException;
import io.telicent.jena.abac.core.VocabAuthzLabels;
import org.apache.jena.atlas.logging.Log;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.impl.Util;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.riot.system.PrefixMap;
import org.apache.jena.riot.system.PrefixMapFactory;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFLib;
import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.TokenType;
import org.apache.jena.riot.tokens.Tokenizer;
import org.apache.jena.riot.tokens.TokenizerText;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.system.G;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Code library for Labels
 */
public class L {

    /**
     * Create an empty, in-memory graph suitable for labels.
     */
    public static Graph newLabelGraph() {
        Graph graph = GraphFactory.createDefaultGraph();
        graph.getPrefixMapping().setNsPrefixes(PrefixesForLabels);
        return graph;
    }

    /**
     * Print the contents of a label store (development helper function).
     */
    @SuppressWarnings("unused")
    public static void printLabelStore(LabelsStore labelStore) {
        PrintStream out = System.out;
        labelStore.forEach((quad, labels) -> out.printf("%-20s %s\n", NodeFmtLib.str(quad), labels));
    }

    /**
     * Quad to string.
     */
    public static String quadToString(Quad quad) {
        // With Turtle abbreviations, e.g. numbers, without prefixes (no rdf:).
        if (Objects.equals(quad.getGraph(), Quad.defaultGraphIRI)) {
            // For quads in the default graph can omit the graph portion of the pattern
            return NodeFmtLib.str(quad.asTriple());
        } else {
            return NodeFmtLib.str(quad);
        }
    }

    // ---- Graph to Labels

    /**
     * Take a graph of labels encoded in RDF and load into a {@link LabelsStore}. Note that this call may need to be
     * enclosed in a transaction.
     */
    public static void loadStoreFromGraph(LabelsStore labelsStore, Graph labelsGraph) {
        BiConsumer<Quad, Label> destination =
                labelsStore::add;
        graphToLabels(labelsGraph, destination);
    }

    /**
     * Parse a labels graph and send labelling to a handler.
     */
    public static void graphToLabels(Graph labelsGraph, BiConsumer<Quad, Label> destination) {
        // [ authz:pattern "" ; authz:label "" ; authz:label ""]
        //    Possibly several authz:label "" per pattern.
        PrefixMap pmap = prefixMap(labelsGraph);
        ExtendedIterator<Triple> patterns = G.find(labelsGraph, null, VocabAuthzLabels.pPattern, null);
        try {
            while (patterns.hasNext()) {
                // The pattern triple.
                Triple t = patterns.next();
                // The node for the pattern-labels
                Node descriptionNode = t.getSubject();
                Node patternStr = t.getObject();
                Quad quad = parsePattern(patternStr, pmap);
                if (!quad.isConcrete()) {
                    throw new LabelsException("Encountered pattern with wildcards: " + NodeFmtLib.str(quad));
                }
                Label label = label(labelsGraph, descriptionNode);
                destination.accept(quad, label);
            }
        } catch (AuthzTriplePatternException ex) {
            String msg = "Pattern: " + ex.getMessage();
            Log.error(Labels.LOG, msg);
            throw new LabelsException(msg, ex);
        } catch (AttributeException ex) {
            String msg = "Label: " + ex.getMessage();
            Log.error(Labels.LOG, msg);
            throw new LabelsException(msg, ex);
        } finally {
            patterns.close();
        }
    }

    public static void labelsToGraph(LabelsStore labelsStore, Graph g) {
        StreamRDF stream = StreamRDFLib.graph(g);
        BiConsumer<Quad, Label> action = (quad, labels) -> asRDF(quad, labels, stream);
        labelsStore.forEach(action);
    }

    // ---- Pattern parser

    /**
     * Turn a pattern string into a QuadPattern
     */
    private static Quad parsePattern(Node pattern, PrefixMap pmap) {
        if (!Util.isSimpleString(pattern)) {
            throw new AuthzTriplePatternException("Not a string literal: " + pattern);
        }
        return parsePattern(pattern.getLiteralLexicalForm(), pmap);
    }

    /**
     * Parses the pattern object of an {@code [] authz:pattern "pattern"} triple
     * <p>
     * Historically RDF-ABAC only supported labelling triples prior to {@code 3.0.0} so this pattern was expected to be
     * three Turtle format nodes separated by whitespace e.g. {@code ex:subject ex:predicate "value"}.  This indicated
     * the triple this pattern declares the label for.
     * </p>
     * <p>
     * The pattern can use Turtle style prefixes with the prefixes being taken from the enclosing graph.  However ,for
     * portability it <strong>MAY</strong> be better to use full URIs to write out pattern components, as that avoids
     * the risk of the patterns being misinterpreted if a prefix is changed/removed.
     * </p>
     * <p>
     * From {@code 3.0.0} onwards the pattern <strong>MAY</strong> now have four components with the first one being
     * treated as the graph e.g. {@code ex:graph ex:subject ex:predicate "value"}.  This means it is not possible to
     * label quads, and thus the same triple in different named graphs can have different labels.
     * </p>
     *
     * @param pattern Pattern literal
     * @param pmap    Prefix map
     * @return Quad to be labelled
     */
    static Quad parsePattern(String pattern, PrefixMap pmap) {
        try {
            // RIOT tokenizer.
            Tokenizer tok = TokenizerText.fromString(pattern);
            Node g = tokenToNode(tok.next(), pmap);
            Node s = tokenToNode(tok.next(), pmap);
            Node p = tokenToNode(tok.next(), pmap);
            if (tok.hasNext()) {
                // New 3.x format where 4 components may be supplied to provide full quad
                Node o = tokenToNode(tok.next(), pmap);
                if (tok.hasNext()) {
                    throw new AuthzTriplePatternException("Extra tokens after pattern");
                }
                return Quad.create(g, s, p, o);
            } else {
                // Pre 3.x format
                // If only 3 components then treat Graph as default graph and components as subject, predicate, object
                return Quad.create(Quad.defaultGraphIRI, g, s, p);
            }
        } catch (RuntimeException ex) {
            String msg = "Bad pattern: \"" + pattern + "\": " + ex.getMessage();
            throw new AuthzTriplePatternException(msg);
        }
    }

    /**
     * Loads the label from an {@code [] authz:label "label"} triple
     * <p>
     * Prior to {@code 3.0.0} the label was only permitted to be a simple literal, and it had to be a valid RDF-ABAC
     * expression.  From {@code 3.0.0} onwards both these constraints are relaxed:
     * </p>
     * <ol>
     *     <li>The label may either be a simple literal, or an {@code xsd:base64Binary} literal.  This allows for
     *     encoding labels with non-printable characters in a reliable and portable way.</li>
     *     <li>The label is no longer validated as an RDF-ABAC label.  This allows a labels graph to convey labels that
     *     use other labelling schemas and/or encodings.</li>
     * </ol>
     * <p>
     * Historically it was permitted to define multiple {@code authz:label} triples for a single pattern.  From
     * {@code 3.0.0} it is now <strong>ONLY</strong> permitted to declare a single label.
     * </p>
     *
     * @param labelsGraph Labels graph
     * @param x           Subject of the label triple
     * @return Label
     */
    private static Label label(Graph labelsGraph, Node x) {
        List<Node> labelNodes = G.listSP(labelsGraph, x, VocabAuthzLabels.pLabel);
        if (labelNodes.size() > 1) {
            throw new AttributeException(
                    "Multiple labels per-triple is no longer permitted, please consolidate into a single label");
        } else if (labelNodes.isEmpty()) {
            throw new AttributeException("No label specified for triple pattern " + NodeFmtLib.strTTL(x));
        }
        Node label = labelNodes.getFirst();
        if (!label.isLiteral()) {
            throw new AttributeException("Label specified as a non-literal for triple pattern " + NodeFmtLib.strTTL(x));
        } else if (Util.isSimpleString(label)) {
            return Label.fromText(label.getLiteralLexicalForm());
        } else if (Objects.equals(label.getLiteralDatatypeURI(), XSD.base64Binary.getURI())) {
            return new Label(Base64.getDecoder().decode(label.getLiteralLexicalForm()), StandardCharsets.UTF_8);
        } else {
            throw new AttributeException(
                    "Label specified as unexpected type (" + label.getLiteralDatatypeURI() + ") for triple pattern " + NodeFmtLib.strTTL(
                            x));
        }
    }

    // Token to node.
    private static Node tokenToNode(Token t, PrefixMap pmap) {
        if (t.getType() == TokenType.UNDERSCORE) {
            return Node.ANY;
        }
        if (t.getType() == TokenType.KEYWORD && t.getImage().equalsIgnoreCase("ANY")) {
            return Node.ANY;
        }
        Node n = t.asNode(pmap);
        if (!n.isURI() && !n.isLiteral() && !n.isBlank()) {
            throw new AuthzTriplePatternException("Node of type (" + t.getType() + " ) not valid in a pattern:: " + n);
        }
        return n;
    }

    // ---- Labels to graph

    static void asRDF(Quad quad, Label label, StreamRDF stream) {
        // Add  [ authz:pattern '...quad...' ;  authz:label "..label.." ] .
        asRDF$(quad, label, stream::triple);
    }

    private static void asRDF$(Quad quad, Label label, Consumer<Triple> output) {
        // Add  [ authz:pattern '...quad...' ;  authz:label "..label.." ] .
        Node x = NodeFactory.createBlankNode();
        output.accept(Triple.create(x, VocabAuthzLabels.pPattern, quadAsNode(quad)));
        output.accept(Triple.create(x, VocabAuthzLabels.pLabel, NodeFactory.createLiteralString(label.getText())));
    }

    private static Node quadAsNode(Quad quad) {
        String s = quadToString(quad);
        return NodeFactory.createLiteralString(s);
    }

    // --- Display related
    public static PrefixMapping PrefixesForLabels = PrefixMapping.Factory.create()
                                                                         .setNsPrefix("rdf", RDF.getURI())
                                                                         .setNsPrefix("xsd", XSD.getURI())
                                                                         .setNsPrefix("authz",
                                                                                      VocabAuthzLabels.getURI())
                                                                         .lock();

    private static PrefixMap prefixMap(Graph graph) {
        return PrefixMapFactory.create(graph.getPrefixMapping());
    }

    /**
     * Check a graph conforms to the expected structure for a graph recording labels.
     *
     * @throws LabelsException Thrown if the given graph is not valid
     */
    public static void checkShape(Graph graph) {
        ExtendedIterator<Triple> iter = G.find(graph, Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        try {
            while (iter.hasNext()) {
                // Triple: ? authz:pattern ?
                Triple triple = iter.next();
                Node subject = triple.getSubject();
                Node object = triple.getObject();
                // Shape

                // Repeats the iterator - is this worth it?
                if (!G.hasOneSP(graph, subject, VocabAuthzLabels.pPattern)) {
                    throw new LabelsException("Multiple patterns for same subject:: " +
                                                      NodeFmtLib.str(subject, prefixMap(graph)));
                }
                // Pattern
                if (!Util.isSimpleString(object)) {
                    // Unexpected compound structure
                    throw new LabelsException("Pattern triple does not have a string as the pattern: " +
                                                      NodeFmtLib.str(object, prefixMap(graph)));
                }

                // Labels.
                List<Node> labels = G.listSP(graph, subject, VocabAuthzLabels.pLabel);
                if (labels.isEmpty()) {
                    throw new LabelsException("No labels for pattern: " +
                                                      NodeFmtLib.str(subject, prefixMap(graph)));
                } else if (labels.size() > 1) {
                    throw new LabelsException("Too many labels (" + labels.size() + ") for pattern: " +
                                                      NodeFmtLib.str(subject, prefixMap(graph)));
                }
                Node label = labels.getFirst();
                if (!label.isLiteral()) {
                    throw new LabelsException(
                            "Non literal label for pattern " + NodeFmtLib.str(subject, prefixMap(graph)));
                } else if (!Util.isSimpleString(label) && !Objects.equals(label.getLiteralDatatypeURI(),
                                                                          XSD.base64Binary.getURI())) {
                    throw new LabelsException(
                            "Literal label not a simple string/base64Binary for pattern " + NodeFmtLib.str(subject,
                                                                                                           prefixMap(
                                                                                                                   graph)));
                }
            }
        } finally {
            iter.close();
        }
    }
}
