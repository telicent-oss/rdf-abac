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

package io.telicent.jena.abac;

import io.telicent.jena.abac.assembler.SecuredDatasetAssembler;
import io.telicent.jena.abac.attributes.syntax.AEX;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.core.VocabAuthzDataset;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsGetter;
import org.apache.jena.atlas.lib.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SysABAC {

    /**
     * Security-Label : The header used to convey the default label that applies to a data payload.
     */
    public static final String hSecurityLabel = "Security-Label";

    /** Constant for "deny all" */
    public static final Label denyLabel = Label.fromText(AEX.strDENY);

    /** Constant for "allow all" */
    public static final Label allowLabel = Label.fromText(AEX.strALLOW);

    /**
     * System-wide default used when there isn't an appropriate label or an error occurred.
     * <p>
     * Normally, a default attribute is associated with {@link DatasetGraphABAC}
     * via the {@link SecuredDatasetAssembler} configuration.
     */
    public static final Label systemDefaultTripleAttributes = denyLabel;

    /**
     * Result if there are no labels or label patterns configured for a dataset.
     * ({@link LabelsGetter} returns null).
     * @implNote
     * Used by {@code SecurityFilterByLabel}.
     */
    public static final boolean DefaultChoiceNoLabels = true;

    public static Logger SYSTEM_LOG = LoggerFactory.getLogger("io.telicent.jena.abac");
    public static Logger DEBUG_LOG = LoggerFactory.getLogger("io.telicent.abac.SecurityFilter");

        /** The product name */
    public static final String NAME         = "RDF ABAC";

    /** Software version taken from the jar file. */
    public static final String VERSION      = Version.versionForClass(SysABAC.class).orElse("<development>");

    public static void init() {
        VocabAuthzDataset.init();
    }
}
