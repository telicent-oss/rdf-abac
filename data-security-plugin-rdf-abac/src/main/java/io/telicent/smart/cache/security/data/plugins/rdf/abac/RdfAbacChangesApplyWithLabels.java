/**
 * Copyright (C) Telicent Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.telicent.smart.cache.security.data.plugins.rdf.abac;

import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.VocabAuthz;
import io.telicent.jena.abac.labels.Label;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.kafka.utils.RDFChangesApplyExternalTransaction;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.sparql.graph.GraphTxn;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A {@link org.apache.jena.rdfpatch.RDFChanges} implementation that honours the transaction semantics of
 * {@link RDFChangesApplyExternalTransaction} as well as updating the {@link io.telicent.jena.abac.labels.LabelsStore}
 * for a {@link DatasetGraphABAC} appropriately
 */
public class RdfAbacChangesApplyWithLabels extends RDFChangesApplyExternalTransaction {

    private final Label securityLabel;
    private final DatasetGraphABAC datasetABAC;
    private final GraphTxn labelsGraph = GraphFactory.createTxnGraph();
    private final Node targetGraph;
    private final Set<Quad> pendingSecurityLabels = new LinkedHashSet<>();

    public RdfAbacChangesApplyWithLabels(DatasetGraphABAC dsgz,
                                         Label securitylabel) {
        this(dsgz, securitylabel, null);
    }

    public RdfAbacChangesApplyWithLabels(DatasetGraphABAC dsgz,
                                         Label securitylabel, String distributionId) {
        super(dsgz);
        this.securityLabel = securitylabel;
        this.datasetABAC = dsgz;
        this.targetGraph = distributionId != null ? NodeFactory.createURI(distributionId) : null;
        this.labelsGraph.begin(TxnType.WRITE);
    }

    @Override
    public void add(Node g, Node s, Node p, Node o) {
        if (VocabAuthz.graphForLabels.equals(g)) {
            // If quad is for labels graph just track that for now
            this.labelsGraph.add(s, p, o);
        } else {
            if (this.targetGraph != null) {
                g = targetGraph;
            } else if (g == null) {
                g = Quad.defaultGraphIRI;
            }
            super.add(g, s, p, o);

            // Apply specific security label if there is one, if not we're relying on the dataset default label applying
            // at read time
            if (securityLabel != null) {
                // An RDF dataset already has set semantics. Mirror those semantics for labels and defer the write so
                // the label store can resolve the label ID and enter its storage transaction once per event.
                this.pendingSecurityLabels.add(Quad.create(g, s, p, o));
            }
        }
    }

    @Override
    public void delete(Node g, Node s, Node p, Node o) {
        if (VocabAuthz.graphForLabels.equals(g)) {
            // If quad is for labels graph just update the labels graph state
            this.labelsGraph.delete(s, p, o);
        } else {
            if (this.targetGraph != null) {
                g = targetGraph;
            } else if (g == null) {
                g = Quad.defaultGraphIRI;
            }
            // Otherwise remove the quad
            // NB - While there is a remove() method on LabelsStore we intentionally don't use it because otherwise a
            //      malicious data producer could remove labels from data by creating a patch that deleted and then re-added
            //      triples
            super.delete(g, s, p, o);
        }
    }

    @Override
    public void txnBegin() {
        // Begin a new transaction first
        super.txnBegin();

        this.pendingSecurityLabels.clear();

        // Begin a transaction on the labels graph
        if (!this.labelsGraph.isInTransaction()) {
            this.labelsGraph.begin(TxnType.WRITE);
        }
    }

    @Override
    public void txnCommit() {
        // Apply the event-level label first, then any explicit labels graph so the explicit labels retain precedence.
        applyPendingSecurityLabels();
        this.labelsGraph.commit();
        applyLabelsGraph();

        // Then apply the commit as normal
        super.txnCommit();
    }

    void applyPendingSecurityLabels() {
        if (this.securityLabel != null && !this.pendingSecurityLabels.isEmpty()) {
            this.datasetABAC.labelsStore().addAll(this.pendingSecurityLabels, this.securityLabel);
            this.pendingSecurityLabels.clear();
        }
    }

    private void applyLabelsGraph() {
        if (!this.labelsGraph.isEmpty()) {
            this.datasetABAC.labelsStore().addGraph(this.labelsGraph);
        }
    }

    @Override
    public void txnAbort() {
        // Abort any changes to the labels graph first
        this.labelsGraph.abort();
        this.pendingSecurityLabels.clear();

        // Then apply the abort as normal
        super.txnAbort();
    }

    @Override
    public void finish() {
        // Upon finish apply the final state of the labels graph
        applyPendingSecurityLabels();
        // Patches applied inside an external transaction may not contain their own TX/TC boundary, leaving the
        // transaction open.  We also cannot commit unconditionally because finish() is also called
        // after txnCommit() or txnAbort(), when the transaction has already ended.
        if (this.labelsGraph.isInTransaction()) {
            this.labelsGraph.commit();
        }
        applyLabelsGraph();
        super.finish();
    }

}
