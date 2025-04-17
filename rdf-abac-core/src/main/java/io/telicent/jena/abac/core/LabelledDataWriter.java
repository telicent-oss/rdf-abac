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

import java.io.OutputStream;
import java.util.List;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.riot.RIOT;
import org.apache.jena.riot.system.StreamRDF;
import org.apache.jena.riot.system.StreamRDFOps;
import org.apache.jena.riot.writer.WriterStreamRDFBase;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.util.Context;

public class LabelledDataWriter {

    public static void writeWithLabels(OutputStream output, DatasetGraphABAC dsgz) {
        Context cxt = RIOT.getContext().copy().set(RIOT.symTurtleDirectiveStyle, "sparql");
        StreamRDF stream = new LDW(output,cxt, dsgz.labelsStore());
        StreamRDFOps.datasetToStream(dsgz.getData(), stream);
    }

    static class LDW extends WriterStreamRDFBase {

        private final LabelsStore labelStore;


        private LDW(OutputStream output, Context context, LabelsStore labelStore) {
            super(output, context);
            this.labelStore = labelStore;
        }

        protected void printTripleNoNL(Triple triple)
        {
            Node s = triple.getSubject() ;
            Node p = triple.getPredicate() ;
            Node o = triple.getObject() ;

            outputNode(s) ;
            out.print(' ') ;
            printProperty(p);
            out.print(' ') ;
            outputNode(o) ;
            //out.println(" .") ;
            // No newline.
            out.print(" .") ;
        }

        @Override
        protected void print(Triple triple) {
            printTripleNoNL(triple);

            List<Label> labels = labelStore.labelsForTriples(triple);
            if ( labels != null && ! labels.isEmpty() ) {
                super.out.pad(50);
                out.print(" //") ;
                labels.forEach(label -> {out.print(" ") ; out.print(label.getText()); });
            }
            out.println();
        }

        @Override
        protected void startData() {}

        @Override
        protected void endData() {}

        @Override
        protected void print(Quad quad) {
            throw new UnsupportedOperationException("Quads in DatasetGraphABAC");
        }

        @Override
        protected void reset() {}
    }
}
