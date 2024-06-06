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

import io.telicent.jena.abac.core.AuthzException;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;

/**
 * Identify the different cases of triples which are stored in the RocksDB label store.
 *
 * The RocksDB implementation of the label store treats each of these different combinations
 * of wildcarded and non-wildcarded SPO triples differently, so it helps to keep the code
 * clean to abstract and extract the pattern once, in a single place.
 */
enum ABACPattern {
            PatternSPO,
            PatternSP_,
            PatternS__,
            Pattern_P_,
            Pattern___;

    static ABACPattern fromTriple(Triple triple) {
        return fromTriple(triple.getSubject(), triple.getPredicate(), triple.getObject());
    }

    static ABACPattern fromTriple(final Node subject, final Node property, final Node object) {
        if (subject != Node.ANY && property != Node.ANY && object != Node.ANY) {
            return PatternSPO;
        }
        if (subject != Node.ANY && property != Node.ANY) {
            return PatternSP_;
        }
        if (subject == Node.ANY && property != Node.ANY && object == Node.ANY) {
            return Pattern_P_;
        }
        if (subject != Node.ANY && object == Node.ANY) {
            return PatternS__;
        }
        if (subject == Node.ANY && property == Node.ANY && object == Node.ANY) {
            return Pattern___;
        }

        throw new AuthzException("Access control rule is not of the form SPO, SP_, S__, _P_ or ___");
    }
}
