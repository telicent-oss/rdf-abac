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
import io.telicent.jena.abac.core.*;
import io.telicent.jena.abac.labels.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFParser;
import org.apache.jena.shacl.ShaclValidator;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFilteredView;
import org.apache.jena.sparql.graph.GraphFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Programmatic API to the Attribute-Based Access Control functionality.
 *
 * @see SecuredDatasetAssembler
 */
public final class ABAC {

    private ABAC() {
    }

    public final static Logger AzLOG = LoggerFactory.getLogger("io.telicent.jena.abac.Authz");
    public final static Logger AttrLOG = LoggerFactory.getLogger("io.telicent.jena.abac.Attribute");

    /**
     * Operate with old style attributes only.
     * This applies to labels, attribute store description, and remote attribute store.
     * No value, no quotes, any character set - no white space or =
     */
    public static boolean LEGACY = true;

    /**
     * Per request label evaluation cache size.
     */
    public static final int labelEvalCacheSize = 100_000;

    /**
     * Per request hierarchy retrieval cache size.
     * This could become a global cache. The answers are not request sensitive.
     */
    public static final int hierarchyCacheSize = 100;

    /**
     * Test whether a dataset supports ABAC data labelling.
     */
    public static boolean isDatasetABAC(DatasetGraph dsg) {
        return dsg instanceof DatasetGraphABAC;
    }

    /**
     * Create a {@link DatasetGraphABAC}.
     */
    public static DatasetGraphABAC authzDataset(DatasetGraph dsgBase, LabelsStore labels, Label datasetDefaultLabel, AttributesStore attributesStore) {
        return authzDataset(dsgBase, null, labels, datasetDefaultLabel, attributesStore);
    }

    /**
     * Create a {@link DatasetGraphABAC}.
     */
    public static DatasetGraphABAC authzDataset(DatasetGraph dsgBase, String accessAttributes,
                                                LabelsStore labels, Label datasetDefaultLabel,
                                                AttributesStore attributesStore) {
        return new DatasetGraphABAC(dsgBase, accessAttributes, labels, datasetDefaultLabel, attributesStore);
    }

    /**
     * Build an attribute-enforcing DatasetGraph. For programmatic/API use,
     * and in unit tests.
     * <p>
     * This is not used by Fuseki.
     */
    public static DatasetGraph requestDataset(DatasetGraphABAC dsgAuthz, AttributeValueSet attributes, HierarchyGetter function) {
        CxtABAC cxt = CxtABAC.context(attributes, function, dsgAuthz.getData());
        return filterDataset(dsgAuthz, cxt);
    }

    /**
     * Build an attribute-enforcing DatasetGraph. For programmatic/API use,
     * and in unit tests.
     * <p>
     * This is not used by Fuseki.
     */
    public static DatasetGraph requestDataset(DatasetGraphABAC dsgAuthz, AttributeValueSet attributes, AttributesStore attrStore) {
        CxtABAC cxt = CxtABAC.context(attributes, attrStore, dsgAuthz.getData());
        return filterDataset(dsgAuthz, cxt);
    }

    /**
     * Create an attribute-filtered dataset for a context.
     *
     * @see #requestDataset
     */
    public static DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
        return filterDataset(dsgAuthz.getData(), dsgAuthz.labelsStore(), dsgAuthz.getDefaultLabel(), cxt);
    }

    /**
     * Create an attribute-filtered dataset for a context.
     * <p>
     * "No label store" (null) is not the same as an empty label store
     * ({@link LabelsStoreZero}). "No store" mean the label filter isn't even
     * incorporated into decisions whereas an empty store may have a default.
     * <p>The DatasetGraph is the data storage dataset.
     */
    public static DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, Label defaultLabel, CxtABAC cxt) {
        QuadFilter filter = null;
        if (labels != null) {
            LabelsGetter getter = labels::labelsForTriples;
            filter = Labels.securityFilterByLabel(dsgBase, getter, defaultLabel, cxt);
        }
        return new DatasetGraphFilteredView(dsgBase, filter, Set.of());
    }

    /**
     * Read SHACL from a classpath resource or file path.
     */
    public static Shapes readSHACL(String resource) {
        Graph gShacl = GraphFactory.createDefaultGraph();
        Shapes shapes = tryClassPath(resource, gShacl);
        if (shapes == null) {
            return tryFilePath(resource, gShacl);
        } else {
            return shapes;
        }
    }

    private static Shapes tryClassPath(String resource, Graph gShacl) {
        try (InputStream in = ABAC.class.getClassLoader().getResourceAsStream(resource)) {
            if (in != null) {
                RDFParser.source(in).lang(Lang.SHACLC).parse(gShacl);
                return ShaclValidator.get().parse(gShacl);
            } else {
                return null;
            }
        } catch (IOException ioex) {
            return null;
        }
    }

    private static Shapes tryFilePath(String resource, Graph gShacl) {
        try (InputStream in = new FileInputStream(resource)) {
            RDFParser.source(in).lang(Lang.SHACLC).parse(gShacl);
            return ShaclValidator.get().parse(gShacl);
        } catch (IOException fnfex) {
            return null;
        }
    }

}
