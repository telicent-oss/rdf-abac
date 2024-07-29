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

import static io.telicent.jena.abac.core.VocabAuthzDataset.*;
import static org.apache.jena.sparql.util.graph.GraphUtils.getAsStringValue;
import static org.apache.jena.sparql.util.graph.GraphUtils.getStringValue;

import io.telicent.jena.abac.core.*;
import org.apache.jena.assembler.exceptions.AssemblerException;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.shared.PropertyNotFoundException;
import org.apache.jena.sparql.util.graph.GraphUtils;

import java.time.Duration;
import java.time.format.DateTimeParseException;

/** Assembler an {@link AttributesStore}, which can be local or remote, cached or not. */
public class AttributeStoreBuildLib {

    /*
     *   [] rdf:type authz:DatasetAuthz ;
     *       authz:labels  <store of labels>      -- default: in-memory store
     *       authz:accessAttributes "" ;          -- default: none
     *       authz:tripleDefaultAttributes "" ;   -- default: null (system default applies)
     *
     *       authz:attributes "filename" ;        -- User attributes and hierarchies
     *       ## OR
     *       ## authz:attributesURL "URL" ;
     *
     *       ## Underlying datasets.
     *       authz:dataset <base data> ;          -- Data
     *       .
     */
    /*package*/ static AttributesStore attributesStore(Resource root) {
        verifyAttributesStore(root);

        boolean localAttributeStore = GraphUtils.getAsRDFNode(root, pAttributes) != null;
        boolean remoteAttributeStore = GraphUtils.getAsRDFNode(root, pAttributesURL) != null;
        boolean cachedAttributeStore = parseBooleanProperty(root, pCachedStore);

        if ( localAttributeStore && remoteAttributeStore )
            throw new AssemblerException(root, "User Attribute Store: Both remote and local local file.");

        AttributesStore store;
        if ( localAttributeStore ) {
            store = localAttributesStore(root);
        } else if ( remoteAttributeStore ) {
            store = remoteAttributesStore(root);
        } else {
            throw new AssemblerException(root, "No attribute store specified.");
        }
        if(cachedAttributeStore) {
            store = cachedAttributesStore(root, store);
        }
        return store;
    }

    private static final Property pNotPluralAttributes = ResourceFactory.createProperty(VocabAuthzDataset.getURI()+"attribute");
    /** Verify the data before processing. */
    private static void verifyAttributesStore(Resource root) {
        if ( null != GraphUtils.getAsRDFNode(root, pNotPluralAttributes) ) {
            throw new AssemblerException(root, "Property \":attribute\" used (singular spelling) where \":attributes\" expected");
        }
    }

    private static AttributesStore remoteAttributesStore(Resource root) {
        // The URL should contain the string "{user}" which is replaced when used with the username from the request.
        // Required.
        String lookupUserTemplate = getAsStringValue(root, pAttributesURL);
        if ( lookupUserTemplate == null )
            return null;
        lookupUserTemplate = environmentValue(root, lookupUserTemplate);

        // Same pattern except this is optional.
        String lookupHierarchyTemplate = getAsStringValue(root, pHierarchiesURL);
        lookupHierarchyTemplate = environmentValue(root, lookupHierarchyTemplate);

        return new AttributesStoreRemote(lookupUserTemplate, lookupHierarchyTemplate);
    }

    /**
     * Process a possible environment variable indirection.
     * Values in System properties are also tried.
     */
    private static String environmentValue(Resource root, String value) {
        if ( value == null )
            return null;
        if ( ! value.startsWith("env:") )
            return value;
        String envVar = value.substring("env:".length());
        String x = lookupEnvironmentVariable(envVar);
        if ( x == null )
            throw new AssemblerException(root, "Bad environment variable for remote user attribute store URL");
        return x;
    }

    private static String lookupEnvironmentVariable(String name) {
        String s1 = System.getenv().get(name);
        if ( s1 != null )
            return s1;
        String s2 = System.getProperty(name);
        return s2;
    }

    /*package*/ static String getTripleDefaultLabel(Resource root) {
        String tripleDefaultAttributes = getStringValue(root, pTripleDefaultLabels);
        if ( tripleDefaultAttributes == null ) {
            // Java ...
            @SuppressWarnings("deprecation")
            String x = getStringValue(root, pTripleDefaultAttributes);
            tripleDefaultAttributes = x;
        }
        if ( tripleDefaultAttributes != null && tripleDefaultAttributes.isEmpty() )
            throw new AssemblerException(root, ":tripleDefaultLabels is an empty string (use \"!\" for 'deny all')");
        return tripleDefaultAttributes;
    }

    /*package*/ static AttributesStore localAttributesStore(Resource root) {
        RDFNode obj = GraphUtils.getAsRDFNode(root, pAttributes);
        if ( obj == null )
            return null;
        return attributesStoreFile(root);
    }

    /*package*/ static AttributesStore cachedAttributesStore(Resource root, AttributesStore store) {
        Duration userAttributeCacheDuration = parseDuration(root, pAttributeCacheExpiry, defaultAttributeCacheExpiry);
        Duration hierarchicalCacheDuration = parseDuration(root, pHierarchyCacheExpiry, defaultHierarchyCacheExpiry);
        long userAttributeCacheSize = parseLongProperty(root, pHierarchyCacheSize, defaultHierarchyCacheSize);
        long hierarchicalCacheSize = parseLongProperty(root, pAttributeCacheSize, defaultAttributeCacheSize);
        return new AttributeStoreCache(store, userAttributeCacheDuration, hierarchicalCacheDuration, userAttributeCacheSize, hierarchicalCacheSize);
    }

    /*package*/ static AttributesStore attributesStoreFile(Resource root) {
        String attributesStoreFilename;
        try {
            attributesStoreFilename = getAsStringValue(root, pAttributes);
            if ( attributesStoreFilename == null )
                return new AttributesStoreLocal();
        } catch(Throwable th) {
            throw new AssemblerException(root, "Attributes store file reference must be an URI or filename string");
        }
        try {
            return Attributes.readAttributesStore(attributesStoreFilename);
        } catch(Throwable th) {
            throw new AssemblerException(root, "Failed to parse the attributes store file '"+attributesStoreFilename+"'", th);
        }
    }

    /*package*/ static Duration parseDuration(Resource root, Property property, Duration defaultDuration) {
        String propertyAsString = GraphUtils.getStringValue(root, property);
        if (null == propertyAsString || propertyAsString.isEmpty()) {
            return defaultDuration;
        }
        try {
            return Duration.parse(propertyAsString);
        } catch (DateTimeParseException ex) {
            throw new AssemblerException(root, "Failed to parse duration format: '" + propertyAsString + "'", ex);
        }
    }

    /*package*/ static boolean parseBooleanProperty(Resource root, Property property) {
        try {
            return GraphUtils.getBooleanValue(root, property);
        } catch (PropertyNotFoundException p) {
            // ignore if not set.
        }
        return false;
    }

    static long parseLongProperty(Resource root, Property property, long defaultValue) {
        try {
            return Long.parseLong(GraphUtils.getStringValue(root, property));
        } catch (PropertyNotFoundException | NumberFormatException e) {
            // ignore if not set.
        }
        return defaultValue;
    }
}
