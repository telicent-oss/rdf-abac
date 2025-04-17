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

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.StreamSplitter;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.node.LabelToNodeGenerator;
import org.apache.jena.atlas.RuntimeIOException;
import org.apache.jena.atlas.io.IO;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.fuseki.servlets.ActionErrorException;
import org.apache.jena.fuseki.servlets.ActionLib;
import org.apache.jena.fuseki.servlets.HttpAction;
import org.apache.jena.fuseki.servlets.ServletOps;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.TxnType;
import org.apache.jena.riot.*;
import org.apache.jena.riot.lang.StreamRDFCounting;
import org.apache.jena.riot.system.*;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.graph.GraphFactory;
import org.apache.jena.web.HttpSC;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

import static java.lang.String.format;

/**
 * The process of loading data with labels.
 */
class LabelledDataLoader {

    // Restructure by splitting up the code
    //   Use with FKProcessorSCG

    private record LoaderRequest(String id, Logger log, DatasetGraphABAC dsgz, InputStream data, String contentTypeStr, String headerLabels) {}

    private record LoaderResponse(long tripleCount, long quadCount, long count, long contentLength, String contentType, Lang lang, String base) {
        public String str() {
            return String.format("Content-Length=%d, Content-Type=%s => %s : Count=%d Triples=%d Quads=%d",
                                 contentLength, contentType,
                                 lang.getName(), count, tripleCount, quadCount);
        }
    }

    /*package*/ static void validate(HttpAction action) {
        DatasetGraph dsg = action.getDataset();
        if ( !(dsg instanceof DatasetGraphABAC) ) {
            // Should have been caught in validate.
            FmtLog.error(action.log, "[%d] This dataset does not support ABAC security labelling.", action.id);
            ServletOps.error(HttpSC.BAD_REQUEST_400, "This dataset does not support ABAC security labelling.");
            return;
        }
    }

    // Current HTTP/ABAC_DataLoader codepath.
    // Called by ABAC_DataLoader.execute()
    /*package*/ static void execute(HttpAction action) {
        DatasetGraph dsg = action.getDataset();
        DatasetGraphABAC dsgz = null;
        if ( dsg instanceof DatasetGraphABAC x )
            dsgz = x;
        if ( dsgz == null ) {
            // Should have been caught in validate.
            FmtLog.error(action.log, "[%d] This dataset does not support ABAC security labelling.", action.id);
            ServletOps.error(HttpSC.BAD_REQUEST_400, "This dataset does not support ABAC security labelling.");
            return;
        }
        action.begin(TxnType.WRITE);
        try {
            // long len = action.getRequestContentLengthLong();

            String hSecurityLabel = action.getRequestHeader(SysABAC.hSecurityLabel.getText());
            List<String> headerSecurityLabels = parseAttributeList(hSecurityLabel);
            Label dsgDftLabel = dsgz.getDefaultLabel();

            if ( headerSecurityLabels != null ) {
                FmtLog.info(action.log, "[%d] Security-Label %s", action.id, headerSecurityLabels);
            } else {
                // Dataset default will apply at use time.
                FmtLog.info(action.log, "[%d] Dataset default label: %s", action.id, dsgDftLabel);
            }
            UploadInfo x = ingestData(action, dsgz, headerSecurityLabels);
            action.log.info(format("[%d] Body: %s", action.id, x.str()));
            action.commit();
            ServletOps.success(action);
            // ServletOps.uploadResponse(action, details);
        } catch (ActionErrorException ex) {
            action.abortSilent();
            throw ex;
        } catch (Throwable ex) {
            action.abortSilent();
            ServletOps.errorOccurred(ex);
        }
    }

    private static List<String> parseAttributeList(String securityLabelsList) {
        if ( securityLabelsList == null ) {
            return null;
        }
        List<AttributeExpr> x = AE.parseExprList(securityLabelsList);
        return AE.asStrings(x);
    }

    private static void applyLabels(DatasetGraphABAC dsgz, Graph labelsGraph) {
        if ( labelsGraph != null && !labelsGraph.isEmpty() )
            dsgz.labelsStore().addGraph(labelsGraph);
    }

    record UploadInfo(long tripleCount, long quadCount, long count, String contentType, long contentLength, Lang lang, String base) {
        public String str() {
            return String.format("Content-Length=%d, Content-Type=%s => %s : Count=%d Triples=%d Quads=%d",
                                 contentLength, contentType, lang,
                                 count, tripleCount, quadCount);
        }
    }

    // ---- Ingestion processing.

    // ==> where?
    /**
     * Ingest labelled data.
     * <p>
     * If it is triples the labels are the ones in the header and can't be in the
     * data body.
     * <p>
     * If it is quads, the data is the default graph and the labels from the header
     * apply but are overridden by the {@code <http://telicent.io/security#labels>}
     * graph.
     */
    /*package*/ static UploadInfo ingestData(HttpAction action, DatasetGraphABAC dsgz, List<String> headerLabels) {
        String base = ActionLib.wholeRequestURL(action.getRequest());
        return ingestData(action, base, dsgz, headerLabels);
    }

    /*package*/ static UploadInfo ingestData(HttpAction action, String base, DatasetGraphABAC dsgz, List<String> headerLabels) {
        try {
            List<Label> labelsToApply = determineLabelsToApply(dsgz.getDefaultLabel(), headerLabels);
            // Decide the label to apply when the data does not explicitly set the
            // labels on a triple.
            Lang lang = RDFLanguages.contentTypeToLang(action.getRequestContentType());
            if ( RDFLanguages.isTriples(lang) ) {
                // Triples. We can stream process the data because we know the label
                // to apply ahead of parsing.
                return ingestTriples(action, lang, base, dsgz, labelsToApply);
            } else if (RDFLanguages.isQuads(lang) ) {
                // Quads. (Currently assumed to be the labels graph). This has to be
                // buffered.
                return ingestQuads(action, lang, base, dsgz, labelsToApply);
            } else {
               ServletOps.errorOccurred("Lang not recognised for processing: " + lang);
            }
        } catch (Throwable ex) {
            throw ex;
        }
        return null;
    }

    private static UploadInfo ingestTriples(HttpAction action, Lang lang, String base, DatasetGraphABAC dsgz, List<Label> headerLabels) {
        StreamRDF baseDest = StreamRDFLib.dataset(dsgz.getData());
        LabelsStore labelsStore = dsgz.labelsStore();
        BiConsumer<Triple, List<Label>> labelledTriplesCollector = labelsStore::add;
        StreamRDF dest = baseDest;
        if ( headerLabels != null && !headerLabels.isEmpty() ) {
            // If there are no header labels, nothing to do - send to base
            dest = new StreamLabeler(baseDest, headerLabels, labelledTriplesCollector);
        }

        StreamRDFCounting countingDest = StreamRDFLib.count(dest);
        parse(action, countingDest, lang, base);

        return new UploadInfo(countingDest.countTriples(), countingDest.countQuads(), countingDest.count(),
                              action.getRequestContentType(), action.getRequestContentLengthLong(), lang, base);
    }

    private static class StreamLabeler extends StreamRDFWrapper {

        private final List<Label> labels;
        private final BiConsumer<Triple, List<Label>> labelsHandler;

        StreamLabeler(StreamRDF destination, List<Label> labels, BiConsumer<Triple, List<Label>> labelsHandler) {
            super(destination);
            this.labels = labels;
            this.labelsHandler = labelsHandler;
        }

        @Override
        public void triple(Triple triple) {
            super.triple(triple);
            labelsHandler.accept(triple, labels);
        }

        @Override
        public void quad(Quad quad) {
            super.quad(quad);
            labelsHandler.accept(quad.asTriple(), labels);
        }
    }

    private static UploadInfo ingestQuads(HttpAction action, Lang lang, String base, DatasetGraphABAC dsgz, List<Label> labelsForData) {
        // We could split the bulk data from the modifications using the fact we are
        // inside a transaction on the dataset. The transaction means we are
        // proceeding optimistically adding to the dataset by streaming to data
        // store, then add labels to the labels store, then commit, making the new
        // triples available in the dataset.
        StreamRDF rdfData = StreamRDFLib.dataset(dsgz.getData());
        // Get all the labels - as they may come before or after the data,
        // we need to collect them together, then process them before the txn commit.
        Graph labelsGraph = GraphFactory.createDefaultGraph();
        StreamRDF stream = new StreamSplitter(rdfData, labelsGraph, labelsForData);
        StreamRDFCounting countingDest = StreamRDFLib.count(stream);
        // Contains: String base = ActionLib.wholeRequestURL(action.getRequest());
        parse(action, countingDest, lang, base);
        applyLabels(dsgz, labelsGraph);
        // UploadDetails is a Fuseki class and has limited accessibility. Convert.
        return new UploadInfo(countingDest.countTriples(), countingDest.countQuads(), countingDest.count(),
                action.getRequestContentType(), action.getRequestContentLengthLong(), lang, base);
    }

    /**
     * Parse RDF content from given input stream.
     * (replicates Jena's ActionLib.parse() method)
     * @throws RiotParseException
     */
    public static void parse(HttpAction action, StreamRDF dest, Lang lang, String base) {
        try {
            InputStream input = action.getRequestInputStream();
            ErrorHandler errorHandler = ErrorHandlerFactory.errorHandlerStd(action.log);
            RDFParser.create()
                    .errorHandler(errorHandler)
                    .labelToNode(LabelToNodeGenerator.generate())
                    .source(input)
                    .lang(lang)
                    .base(base)
                    .parse(dest);
        } catch (RuntimeIOException ex) {
            if ( ex.getCause() instanceof CharacterCodingException)
                throw new RiotException("Character Coding Error: "+ex.getMessage());
            throw ex;
        } catch (IOException ex) {
            IO.exception(ex);
        }
    }

    /**
     * Check the incoming labels to see if they match the existing default on the dataset.
     * In which case, we do not need to apply them. This means we save space in the Label Store
     * and reduce the amount of processing required on the data set.
     *
     * Note: This does not affect explicitly set labels (i.e. quads) or in named graphs.
     * Only on triples.
     * @param datasetDefaultLabel dataset's default label.
     * @param providedHeaderLabels The default label provided in the upload call.
     * @return Labels to apply - or empty if not needed.
     */
    private static List<Label> determineLabelsToApply(Label datasetDefaultLabel, List<String> providedHeaderLabels) {
        if (providedHeaderLabels != null) {
            if(!providedHeaderLabels.isEmpty()) {
                if (datasetDefaultLabel != null && providedHeaderLabels.get(0).equalsIgnoreCase(datasetDefaultLabel.getText())) {
                    return Collections.emptyList();
                }
            }
            return providedHeaderLabels.stream().map(Label::fromText).toList();
        } else {
            return Collections.emptyList();
        }
    }

}
