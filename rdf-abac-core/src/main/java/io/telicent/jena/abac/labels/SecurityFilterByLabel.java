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

import static org.apache.jena.riot.out.NodeFmtLib.str;

import java.util.List;

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.attributes.AttributeException;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.QuadFilter;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;

// Give it a name! Makes it findable by Class hierarchy for QuadFilter
/*package*/ class SecurityFilterByLabel implements QuadFilter {
    static final Logger logFilter = SysABAC.DEBUG_LOG;

    private final LabelsGetter labels;
    private final List<String> defaultLookup;
    private final CxtABAC cxt;
    private final boolean debug;

    // Test and development help (prefer CxtABAC.systemTrace)
    private static boolean generalDebug = false;
    /*package*/ static void setDebug(boolean value) { generalDebug = value; }
    /*package*/ static boolean getDebug() { return generalDebug; }

    SecurityFilterByLabel(DatasetGraph dsgBase, LabelsGetter labels, String defaultLabel, CxtABAC cxt) {
        this.labels = labels;
        this.defaultLookup = (defaultLabel == null)
                ? List.of(SysABAC.systemDefaultTripleAttributes)
                : List.of(defaultLabel);
        this.cxt = cxt;
        this.debug = generalDebug ? true : cxt.debug();
    }

    // [ABAC] optimize! cache parsing of labels. cache evaluation per-request needed!

    @Override
    public boolean test(Quad quad) {
        Triple triple = quad.asTriple();

        List<String> dataLabels = labels.apply(triple);
        if ( dataLabels == null ) {
            // No labels configured
            if ( debug )
                FmtLog.info(logFilter, "  No labels configured : %s", SysABAC.DefaultChoiceNoLabels);
            return SysABAC.DefaultChoiceNoLabels;
        }

        boolean noLabelForTriple = dataLabels.isEmpty();
        // No labels given for this quad.
        if ( noLabelForTriple )
            dataLabels = defaultLookup;

        // User: cxt.
        AttributeValueSet requestAttr = cxt.requestAttributes();

        if ( debug ) {
            String x = noLabelForTriple ? "Default:" : "";
            FmtLog.info(logFilter, "(%s) : %s%s", str(triple), x, dataLabels);
        }

        boolean b = determineOutcome(cxt, debug, dataLabels, requestAttr);
        if ( debug ) {
            String x = noLabelForTriple ? "Default:" : "";
            FmtLog.info(logFilter, "(%s) : %s%s --> %s", str(triple), x, dataLabels, b);
        }
        return b;
    }

    private static boolean determineOutcome(CxtABAC cxt, boolean debug, List<String> dataLabels, AttributeValueSet reqAttr) {
        // -- Concrete quoted triple
        // When there is more than one label attribute on the
        // data, all expression must pass.
        for(String dataLabel : dataLabels ) {
            Cache<String, ValueTerm> cache = cxt.labelEvalCache();
            ValueTerm value = cache.get(dataLabel, (dLabel)->eval1(cxt, debug, dLabel, reqAttr));
            if ( ! value.getBoolean() )
                return false;
        }
        return true;
    }

    private static ValueTerm eval1(CxtABAC cxt, boolean debug, String dataLabel, AttributeValueSet reqAttr) {
      AttributeExpr aExpr = AE.parseExpr(dataLabel);
      // The Hierarchy handling code is in AttrExprEvaluator
      ValueTerm value = aExpr.eval(cxt);
      if ( value == null )
          throw new AttributeException("Null return from AttributeExpr.eval");
      if ( ! value.isBoolean() )
          throw new AttributeException("Not a boolean: eval of "+aExpr);
      return value;
    }
}
