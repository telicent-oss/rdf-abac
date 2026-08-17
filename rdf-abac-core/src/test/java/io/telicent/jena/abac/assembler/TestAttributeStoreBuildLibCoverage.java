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

package io.telicent.jena.abac.assembler;

import io.telicent.jena.abac.labels.Label;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static io.telicent.jena.abac.core.VocabAuthzDataset.pTripleDefaultLabels;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
@SuppressWarnings("java:S5778")
class TestAttributeStoreBuildLibCoverage {

    @Test
    void utilityConstructor_isCallableReflectively() throws Exception {
        var ctor = AttributeStoreBuildLib.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        assertNotNull(ctor.newInstance());
    }

    @Test
    void getTripleDefaultLabel_handlesMissingAndPresentValues() {
        Resource root = ModelFactory.createDefaultModel().createResource();
        assertNull(AttributeStoreBuildLib.getTripleDefaultLabel(root));

        root.addProperty(pTripleDefaultLabels, "alpha");
        assertEquals(Label.fromText("alpha"), AttributeStoreBuildLib.getTripleDefaultLabel(root));
    }

    @Test
    void parseDuration_usesDefaultAndRejectsBadValues() {
        Resource root = ModelFactory.createDefaultModel().createResource();
        assertEquals(Duration.ofSeconds(9),
                     AttributeStoreBuildLib.parseDuration(root, io.telicent.jena.abac.core.VocabAuthzDataset.pAttributeCacheExpiry,
                                                          Duration.ofSeconds(9)));

        root.addProperty(io.telicent.jena.abac.core.VocabAuthzDataset.pAttributeCacheExpiry, "bad");
        assertThrows(AssemblerException.class,
                     () -> AttributeStoreBuildLib.parseDuration(root,
                                                                io.telicent.jena.abac.core.VocabAuthzDataset.pAttributeCacheExpiry,
                                                                Duration.ofSeconds(1)));
    }
}
