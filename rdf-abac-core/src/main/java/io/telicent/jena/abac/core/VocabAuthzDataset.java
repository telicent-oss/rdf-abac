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

import io.telicent.jena.abac.assembler.SecuredDatasetAssembler;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.core.assembler.AssemblerUtils;

/**
 * Vocabulary for assembler for authz datasets.
 */
public class VocabAuthzDataset {

    private static final String NS = VocabAuthz.getURI();
    public static String getURI() {
        return VocabAuthz.getURI();
    }

    public static Resource tDatasetAuthz = ResourceFactory.createResource(NS+"DatasetAuthz");

    /** The underlying dataset being wrapped in authorization. */
    public static Property pDataset = ResourceFactory.createProperty(NS+"dataset");

    /**
     * Attributes required for accessing the data.
     * Failure to match will return a 403.
     */
    public static Property pAccessAttributes = ResourceFactory.createProperty(NS+"accessAttributes");

    // ---- Attributes Store
    //   One of pAttributes, pAttributesStoreURL
    //        pAttributes - local file
    //        pAttributesURL - URL to an external service

    //   Optional pHierarchiesURL

    //   One of pLabels, pLabelsStore
    //     If pLabelsStore, then [ pLabelsStorePath .... ]

    //   Optional pTripleDefaultLabels

    /**
     * Property to refer to a resource for the definition of the attribute store.
     * An alternative to defining on the DatasetGraphABAC assembler.
     */
    public static Property pAttributesStore = ResourceFactory.createProperty(NS+"attributesStore");

    /**
     * Attribute Store: either a file name, which is read into an in-memory or a
     * dataset in this assembler where the default graph holds the attributes
     * configuration.
     */
    public static Property pAttributes = ResourceFactory.createProperty(NS+"attributes");

    /**
     * Attribute Store: Remote store - URL of the user attribute lookup service.
     */
    public static Property pAttributesURL = ResourceFactory.createProperty(NS+"attributesURL");

    /**
     * Attribute Store: Remote store - URL of the hierarchy lookup service.
     */
    public static Property pHierarchiesURL = ResourceFactory.createProperty(NS+"hierarchiesURL");

    // ---- Labels Store

    /**
     * Property to refer to a labels store definition
     * (if not part of the authz:DatasetAuthz).
     */
    public static Property pLabelsStore = ResourceFactory.createProperty(NS+"labelsStore");

    /**
     * Labels: Direct link to a file name, which is read into an in-memory labels datastructure.
     */
    public static Property pLabels = ResourceFactory.createProperty(NS+"labels");

    // RocksDB-based label store.
    /**
     * Property to refer to directory for the RicksDB database.
     */
    public static Property pLabelsStorePath = ResourceFactory.createProperty(NS+"labelsStorePath");

    // -- Dataset attribute settings.

    /**
     * Attributes for when a triple has no matching attribute expression.
     * Failure to match will hide the triple.
     */
    public static Property pTripleDefaultLabels = ResourceFactory.createProperty(NS+"tripleDefaultLabels");

    /** @deprecated Use {@link #pTripleDefaultLabels} */
    @Deprecated
    public static Property pTripleDefaultAttributes = ResourceFactory.createProperty(NS+"tripleDefaultAttributes");

   private static boolean initialized = false ;

    static { init() ; }

    static synchronized public void init() {
        if ( initialized )
            return;
        initialized = true;
        AssemblerUtils.registerDataset(tDatasetAuthz, new SecuredDatasetAssembler());
    }
}
