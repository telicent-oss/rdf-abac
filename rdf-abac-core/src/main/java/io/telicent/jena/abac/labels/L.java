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

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.attributes.AttributeException;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.core.VocabAuthzLabels;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.atlas.logging.FmtLog;
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
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.system.G;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.XSD;

import java.io.PrintStream;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Code library for Labels */
public class L {

    /** Create an empty, in-memory graph suitable for labels. */
    public static Graph newLabelGraph() {
        Graph graph = GraphFactory.createDefaultGraph();
        graph.getPrefixMapping().setNsPrefixes(PrefixesForLabels);
        return graph;
    }

    public static String displayString(Triple triple) {
        return
            NodeFmtLib.str(triple.getSubject(), PrefixMapForLabels)
            + " "
            + NodeFmtLib.str(triple.getPredicate(), PrefixMapForLabels)
            + " "
            + NodeFmtLib.str(triple.getObject(), PrefixMapForLabels);
    }

    /**
     * Print the contents of a label store (development helper function).
     */
    public static void printLabelStore(LabelsStore labelStore) {
        PrintStream out = System.out;
        labelStore.forEach((triple, labels) ->{
            out.printf("%-20s %s\n", NodeFmtLib.str(triple), labels);
        });
    }

    /*packlage*/ static List<String> combineLabelsLists(List<String> labels, List<String> current) {
        // Assume short lists!
        List<String> merge = new ArrayList<>(labels);
        current.forEach(x->{
           if ( ! merge.contains(x) )
               merge.add(x);
        });
        return merge;
    }

    /** Check whether a triple pattern is concrete. */
    /*package*/ static boolean isConcreteTriple(Triple triple) {
        // All SPO defined
        Objects.requireNonNull(triple);
        return triple.getSubject().isConcrete()
                && triple.getPredicate().isConcrete()
                && triple.getObject().isConcrete();
    }

    /** Check whether a triple pattern is indexable. */
    /*package*/ static boolean isPatternTriple(Triple triple) {
        // SPO, SP, S or P, or ANY
        Objects.requireNonNull(triple);

        if ( triple.getSubject().isConcrete() )
            // SPO, SP, S
            return true;
        if ( triple.getPredicate().isConcrete() )
            // P
            return true;
        if ( triple.equals(Triple.ANY) )
            // ANY
            return true;
        return false;
    }

    /** Triple pattern to string. */
    public static String tripleToString(TriplePattern triplePattern) {
        // With Turtle abbreviations, e.g. numbers, without prefixes (no rdf:).
        return triplePattern.str();
    }

    /** Triple to string. */
    public static String tripleToString(Triple triple) {
        // With Turtle abbreviations, e.g. numbers, without prefixes (no rdf:).
        String s = NodeFmtLib.str(triple);
        return s;
    }

    /** Triple to node (string as literal). */
    public static Node tripleToNode(Triple triple) {
        String s = tripleToString(triple);
        Node n = NodeFactory.createLiteralString(s);
        return n;
    }

    // ---- Graph to Labels

    /**
     * Take a graph of labels encoded in RDF and load into a {@link LabelsStore}.
     * Note that this call may need to be enclosed in a transaction.
     */
    public static void loadStoreFromGraph(LabelsStore labelsStore, Graph labelsGraph) {
        BiConsumer<TriplePattern, List<Label>> destination =
                (pattern, labels) -> {
                    Triple t = pattern.asTriple();
                    labelsStore.add(t, labels);
                };
        graphToLabels(labelsGraph, destination);
    }

    /**
     * Parse a labels graph and send labelling to a handler.
     */
    public static void graphToLabels(Graph labelsGraph, BiConsumer<TriplePattern, List<Label>> destination) {
        // [ authz:pattern "" ; authz:label "" ; authz:label ""]
        //    Possibly several authz:label "" per pattern.
        PrefixMap pmap =prefixMap(labelsGraph) ;
        ExtendedIterator<Triple> patterns = G.find(labelsGraph, null, VocabAuthzLabels.pPattern, null);
        try {
            while(patterns.hasNext()) {
                // The pattern triple.
                Triple t = patterns.next();
                // The node for the pattern-labels
                Node descriptionNode = t.getSubject();
                Node patternStr = t.getObject();
                TriplePattern pattern = parsePattern(patternStr, pmap);
                List<Label> labels = attributeExpressions(labelsGraph, descriptionNode);
                destination.accept(pattern, labels);
            }
        } catch (AuthzTriplePatternException ex) {
            String msg = "Pattern: "+ ex.getMessage();
            Log.error(Labels.LOG, msg);
            throw new LabelsException(msg, ex);
        } catch (AttributeException ex) {
            String msg = "Label: "+ex.getMessage();
            Log.error(Labels.LOG, msg);
            throw new LabelsException(msg, ex);
        } finally { patterns.close(); }
    }

    public static Graph labelsToGraph(LabelsStore labelsStore) {
        Graph g = GraphFactory.createGraphMem();
        labelsToGraph(labelsStore, g);
        return g;
    }

    public static void labelsToGraph(LabelsStore labelsStore, Graph g) {
        StreamRDF stream = StreamRDFLib.graph(g);
        BiConsumer<Triple, List<Label>> action = (triple, labels) -> {
            asRDF(triple, labels, stream);
        };
        labelsStore.forEach(action);
    }

    // ---- Pattern parser
    /** Turn a pattern string into a TriplePattern */
    private static TriplePattern parsePattern(Node pattern, PrefixMap pmap) {
        if ( ! Util.isSimpleString(pattern) )
            throw new AuthzTriplePatternException("Not a string literal: "+pattern);
        return parsePattern(pattern.getLiteralLexicalForm(), pmap);
    }

    static TriplePattern parsePattern(String pattern, PrefixMap pmap) {
        try {
            // RIOT tokenizer.
            Tokenizer tok = TokenizerText.fromString(pattern);
            Node s = tokenToNode(tok.next(), pmap);
            Node p = tokenToNode(tok.next(), pmap);
            Node o = tokenToNode(tok.next(), pmap);
            if ( tok.hasNext() )
                throw new AuthzTriplePatternException("Extra tokens after pattern");
            return TriplePattern.create(s,p,o);
        }
        catch (RuntimeException ex) {
            String msg =  "Bad pattern: \""+pattern+"\": "+ex.getMessage();
            //Log.error(LabelsIndex.LOG, msg);
            throw new AuthzTriplePatternException(msg);
        }
    }

    // ---- String labels to attribute expressions
    // Alternative
    /** Fetch and parse the labels (attribute expressions) of node x */
//    private static List<AttributeExpr> attributeExpressions(Graph labelsGraph, Node x) {
//        List<Node> attrLabelNodes = G.listSP(labelsGraph, x , VocabAuthzLabels.pLabel);
//        List<AttributeExpr> attrLabels = new ArrayList<>(attrLabelNodes.size());
//        for ( Node n : attrLabelNodes ) {
//            if ( ! Util.isSimpleString(n) )
//                throw new AttributeException("Not a string literal: "+n );
//            String label = n.getLiteralLexicalForm();
//            AttributeExpr attrExpr = AttributeParser.parseExpr(label);
//            attrLabels.add(attrExpr);
//        }
//        return attrLabels;
//    }

    // Check but still strings version.
    private static List<Label> attributeExpressions(Graph labelsGraph, Node x) {
        List<Node> attrLabelNodes = G.listSP(labelsGraph, x , VocabAuthzLabels.pLabel);
        List<Label> attrLabels = new ArrayList<>(attrLabelNodes.size());
        for ( Node n : attrLabelNodes ) {
            if ( ! Util.isSimpleString(n) ) {
                throw new AttributeException("Not a string literal: " + n);
            }
            Label label = Label.fromText(n.getLiteralLexicalForm());
            // Parse it to check it is legal syntax
            AttributeExpr attrExpr = parseAttrExpr(label.getText());
            // We could store the parsed form.
            //attrLabels.add(attrExpr);
            attrLabels.add(label);
        }
        return attrLabels;
    }

    // Token to node.
    private static Node tokenToNode(Token t, PrefixMap pmap) {
        if ( t.getType() == TokenType.UNDERSCORE )
            return Node.ANY;
        if ( t.getType() == TokenType.KEYWORD && t.getImage().equalsIgnoreCase("ANY") )
            return Node.ANY;
        Node n = t.asNode(pmap);
        if ( ! n.isURI() && ! n.isLiteral() && ! n.isBlank() )
            throw new AuthzTriplePatternException("Node of type (" + t.getType() + " ) not valid in a pattern:: "+n);
        return n;
    }

    // Cached parsing on attribute expressions.
    // Use a small cache to cover the common case of all the labels being the same.
    private static final Cache<String, AttributeExpr> parserCache = CacheFactory.createOneSlotCache();
    /** Parse an attribute expressions - a label */
    private static AttributeExpr parseAttrExpr(String str) {
        return parserCache.get(str, (k)->AE.parseExpr(k));
    }

    // ---- Labels to graph

    // Concrete version
    // See also PatternIndex.toGraph
    // XXX Combine ways to publish as RDF
    /*package*/ static void asRDF(Triple triple, List<Label> labels, StreamRDF stream) {
        // Add  [ authz:pattern '...triple...' ;  authz:label "..label.." ] .
        asRDF$(triple, labels, stream::triple);
    }

    /*package*/ static void asRDF(Triple triple, List<Label> labels, Graph graph) {
        // Add  [ authz:pattern '...triple...' ;  authz:label "..label.." ] .
        asRDF$(triple, labels, graph::add);
    }

    private static void asRDF$(Triple triple, List<Label> labels, Consumer<Triple> output) {
        // Add  [ authz:pattern '...triple...' ;  authz:label "..label.." ] .
        Node x = NodeFactory.createBlankNode();
        Triple tPattern = Triple.create(x, VocabAuthzLabels.pPattern, tripleAsNode(triple));
        output.accept(tPattern);
        for ( Label label : labels ) {
            Triple tLabel = Triple.create(x, VocabAuthzLabels.pLabel, NodeFactory.createLiteralString(label.getText()));
            output.accept(tLabel);
        }
    }

    /*package*/ static void asRDF(TriplePattern triplePattern, List<Label> labels, StreamRDF stream) {
        // Add  [ authz:pattern '...triple...' ;  authz:label "..label.." ] .
        Node x = NodeFactory.createBlankNode();
        Triple tPattern = Triple.create(x, VocabAuthzLabels.pPattern, patternAsNode(triplePattern));
        stream.triple(tPattern);
        for ( Label label : labels ) {
            Node obj = NodeFactory.createLiteralString(label.getText());
            Triple tLabel = Triple.create(x, VocabAuthzLabels.pLabel, obj);
            stream.triple(tLabel);
        }
    }

    private static Node patternAsNode(TriplePattern triplePattern) {
        String s = triplePattern.str();
        return NodeFactory.createLiteralString(s);
    }

    private static Node tripleAsNode(Triple triple) {
        String s = tripleToString(triple);
        return NodeFactory.createLiteralString(s);
    }

    // --- Display related
    public static PrefixMapping PrefixesForLabels =  PrefixMapping.Factory.create()
            .setNsPrefix( "rdf", RDF.getURI() )
            .setNsPrefix( "xsd", XSD.getURI() )
            .setNsPrefix( "authz", VocabAuthzLabels.getURI() )
            .lock();

    // Sad.
    private static PrefixMap PrefixMapForLabels = prefixMapForLabels();
    private static PrefixMap prefixMapForLabels() {
        PrefixMap prefixMap = PrefixMapFactory.create();
        prefixMap.add( "rdf", RDF.getURI() );
        prefixMap.add( "xsd", XSD.getURI() );
        prefixMap.add( "authz", VocabAuthzLabels.getURI() );
        return prefixMap;
    }

    private static PrefixMap prefixMap(Graph graph) {
        return PrefixMapFactory.create(graph.getPrefixMapping());
    }

    // [ authz:pattern "" ; authz:label "" ; authz:label ""]
    // one pattern, one or more labels.
    /**
     * Check a graph conforms to the expected structure for a graph recording labels.
     */
    public static void checkShape(Graph graph) {
        ExtendedIterator<Triple> iter = G.find(graph, Node.ANY, VocabAuthzLabels.pPattern, Node.ANY);
        try {
            while(iter.hasNext() ) {
                Triple triple = iter.next();        // Triple: ? authz:pattern ?
                Node subject = triple.getSubject();
                Node object = triple.getObject();
                boolean isOK = true;
                // Shape

                // Repeats the iterator - is this worth it?
                if ( ! G.hasOneSP(graph, subject, VocabAuthzLabels.pPattern) ) {
                    FmtLog.error(SysABAC.SYSTEM_LOG, "Multiple patterns for same subject:: %s", NodeFmtLib.str(subject, prefixMap(graph)));
                    // XXX throw
                    continue;
                }
                // Pattern
                if ( ! Util.isSimpleString(object) ) {
                    // Unexpected compound structure
                    FmtLog.error(SysABAC.SYSTEM_LOG, "Pattern triple does not have a string as the pattern: %s", NodeFmtLib.str(object, prefixMap(graph)));
                    // XXX throw
                    continue;
                }
                String patternStr = object.getLiteralLexicalForm();

                if ( ! checkPatternString(patternStr) ) {
                    FmtLog.error(SysABAC.SYSTEM_LOG, "Bad pattern: %s: pattern='%s'", NodeFmtLib.str(subject, prefixMap(graph)), patternStr);
                    // XXX throw
                    continue;
                }

//                // Parse the string
//                Triple tripleFromPattern = parse the string.
//                if (! L.isPatternTriple(tripleFromPattern) ) {
//                    // ERROR
//                }

                // Labels.
                List<Node> labels = G.listSP(graph, subject, VocabAuthzLabels.pLabel);
                if ( labels.isEmpty() ) {
                    FmtLog.error(SysABAC.SYSTEM_LOG, "No labels for pattern: %s", NodeFmtLib.str(subject, prefixMap(graph)));
                    // XXX throw
                    continue;
                }

                labels.forEach(label-> {
                    if ( ! checkLabel(label) )
                        FmtLog.error(SysABAC.SYSTEM_LOG, "Bad label: %s : label=%s", NodeFmtLib.str(subject, prefixMap(graph)), label);
                } );
                // OK!
            }
        } finally { iter.close(); }
    }

    /**
     * Check the string - return true if acceptable
     */
    private static boolean checkPatternString(String patternStr) {
        // XXX Check the string
        return true;
    }

    // Use a single slot cache to cover the common case of all the labels being the same.
    private static Cache<Label, Boolean> cacheValidation = CacheFactory.createOneSlotCache();
    private static Cache<Label, Boolean> nonCacheValidation = CacheFactory.createNullCache();

    /**
     * Check the labels.
     * <p>
     * Checking is "best effort" combined with low cost for the common case of bursts
     * of triples with the same label.
     * @throws LabelsException
     */
    public static void validateLabels(List<Label> labels) {
        if ( labels.isEmpty() ) {
            return;
        }
        if ( labels.size() == 1 ) {
            // List of one - common - fastpath
            var a = labels.get(0);
            if ( ! checkLabel(a, cacheValidation) ) {
                throw new LabelsException("Bad label: " + a);
            }
            return ;
        }

        // Multiple labels.
        Set<Label> elts = new HashSet<>(labels.size());
        elts.addAll(labels);
        if ( elts.size() != labels.size() ) {
            throw new LabelsException("Duplicates in labels list: " + labels);
        }
        labels.forEach(a-> {
            if ( ! checkLabel(a, nonCacheValidation) ) {
                throw new LabelsException("Bad label: " + a);
            }
        });
    }

    /**
     * Check a label.
     * Bad labels are logged.
     * Returns true/false.
     */
    private static boolean checkLabel(Label label, Cache<Label, Boolean> cache) {
        Boolean bool = cache.get(label, L::parse1);
        return bool;
    }

    /**
     * Check a label in the form of a Node.
     */
    private static boolean checkLabel(Node labelNode) {
        if ( ! Util.isSimpleString(labelNode) )
            return false;
        return checkLabel(Label.fromText(labelNode.getLiteralLexicalForm()), cacheValidation);
    }

    private static Boolean parse1(Label label) {
        try {
            // Bad labels are logged.
            /*AttributeExpr aExpr =*/ AE.parseExpr(label.getText());
            return Boolean.TRUE;
        } catch (AttributeException ex) {
            return Boolean.FALSE;
        }
    }

}
