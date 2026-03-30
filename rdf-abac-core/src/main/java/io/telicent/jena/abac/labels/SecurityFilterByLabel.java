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

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.attributes.AttributeException;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.QuadFilter;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.logging.FmtLog;
import org.apache.jena.sparql.core.Quad;
import org.slf4j.Logger;

// Give it a name! Makes it findable by Class hierarchy for QuadFilter
class SecurityFilterByLabel implements QuadFilter {
    static final Logger logFilter = SysABAC.DEBUG_LOG;

    private final LabelsGetter labels;
    private final Label defaultLabel;
    private final CxtABAC cxt;
    private final boolean debug;

    // Test and development help (prefer CxtABAC.systemTrace)
    private static boolean generalDebug = false;

    static void setDebug(boolean value) {
        generalDebug = value;
    }

    static boolean getDebug() {
        return generalDebug;
    }

    SecurityFilterByLabel(LabelsGetter labels, Label defaultLabel, CxtABAC cxt) {
        this.labels = labels;
        this.defaultLabel = (defaultLabel == null)
                            ? SysABAC.systemDefaultTripleAttributes
                            : defaultLabel;
        this.cxt = cxt;
        this.debug = generalDebug || cxt.debug();
    }

    // [ABAC] optimize! cache parsing of labels. cache evaluation per-request needed!

    @Override
    public boolean test(Quad quad) {
        Label dataLabel = labels.apply(quad);
        boolean noLabelForQuad = false;
        if (dataLabel == null) {
            dataLabel = defaultLabel;
            noLabelForQuad = true;
        }

        // User: cxt.

        if (debug) {
            String x = noLabelForQuad ? "Default:" : "";
            FmtLog.info(logFilter, "(%s) : %s%s", str(quad), x, dataLabel);
        }

        boolean b = determineOutcome(cxt, dataLabel);
        if (debug) {
            String x = noLabelForQuad ? "Default:" : "";
            FmtLog.info(logFilter, "(%s) : %s%s --> %s", str(quad), x, dataLabel, b);
        }
        return b;
    }

    private static boolean determineOutcome(CxtABAC cxt, Label dataLabel) {
        Cache<Label, ValueTerm> cache = cxt.labelEvalCache();
        ValueTerm value = cache.get(dataLabel, (dLabel) -> eval1(cxt, dLabel));
        return value.getBoolean();
    }

    private static ValueTerm eval1(CxtABAC cxt, Label dataLabel) {
        AttributeExpr aExpr = AE.parseExpr(dataLabel.getText());
        // The Hierarchy handling code is in AttrExprEvaluator
        ValueTerm value = aExpr.eval(cxt);
        if (value == null) {
            throw new AttributeException("Null return from AttributeExpr.eval");
        }
        if (!value.isBoolean()) {
            throw new AttributeException("Not a boolean: eval of " + aExpr);
        }
        return value;
    }
}
