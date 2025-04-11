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

import java.util.*;
import java.util.concurrent.TimeUnit;

import com.github.benmanes.caffeine.cache.Caffeine;

import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.labels.Label;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.atlas.lib.cache.CacheCaffeine;
import org.apache.jena.sparql.core.DatasetGraph;

/**
 * Context for an ABAC execution.
 * A new context is created for each request.
 * There is a requestId - a unique key that can be used to identity the request.
 */
public class CxtABAC {

    public static void systemTrace(Track trace) { globalTrace = trace; }
    private static Track globalTrace = Track.NONE;

    // Unique request id.
    private final UUID id;

    /** The attributes values associated with the request. */
    private final AttributeValueSet requestAttributes;

    /** Accessor: Function to get attribute hierarchies */
    private final HierarchyGetter attrHierarchy;

    // --- Per request caches

    /** Cache of evaluations within this request context. */
    private final Cache<Label, ValueTerm> evalCache;

    /** Hierarchy lookup cache. Cache can't hold nulls. */
    private final Cache<Attribute, Optional<Hierarchy>> hierarchyCache;

    /** The data being protected. */
    private final DatasetGraph baseData;
    /** Request environment values. These are not the attributes of the user.*/
    private final Map<Attribute, ValueTerm> environment;

    private Track trace;

    public static CxtABAC context(AttributeValueSet requestAttributes,
                                  HierarchyGetter attrHierarchy,
                                  DatasetGraph dsgBase) {
        Objects.requireNonNull(requestAttributes);
        Objects.requireNonNull(attrHierarchy);
        // No environment support yet.
        // No security by rdf:type yet.
        return context(requestAttributes, attrHierarchy, null, dsgBase);
    }

    private static CxtABAC context(AttributeValueSet requestAttributes,
                                   HierarchyGetter attrHierarchy,
                                   Map<Attribute, ValueTerm> environment,
                                   DatasetGraph dsgBase) {
        return new CxtABAC(requestAttributes, attrHierarchy, environment, dsgBase);
    }

    private CxtABAC(AttributeValueSet requestAttributes, HierarchyGetter attrHierarchy,
                    Map<Attribute, ValueTerm> environment,
                    DatasetGraph baseData) {
        this.requestAttributes = requestAttributes;
        this.attrHierarchy = attrHierarchy;

        this.evalCache = createEvalCache(ABAC.labelEvalCacheSize);
        // Having a label eval cache means the hierarchy cache is less important
        // because there are often (?) only a few distinct labels using the hierarchy
        // across the data.
        this.hierarchyCache = createHierarchyCache(ABAC.hierarchyCacheSize);

        this.baseData = baseData;
        this.environment = environment;
        this.trace = globalTrace;
        this.id = UUID.randomUUID();
    }

    static <X,Y> Cache<X,Y> createEvalCache(int size) {
        if ( size <= 0 ) {
            return CacheFactory.createNullCache();
        }
        // This cache is for "label" + user attributes evaluating to a result -
        // fairly lightweight calculation, that may be made a lot in a request.
        //
        // CxtABAC is only used within a request execution so the cache is same-thread.
        // This is a good choice because each request renews the security state,
        // immediately picking up the current attributes for the user.
        //
        // The simple cache is low overhead. An alternative is to have an entry
        // timeout multi-thread cache e.g. one based on Guava or Caffeine. They have
        // higher overheads for better caching efficiency.
        return CacheFactory.createSimpleCache(size);
    }

    static <X,Y> Cache<X,Y> createHierarchyCache(int size) {
        if ( size <= 0 ) {
            return CacheFactory.createNullCache();
        }
        // The hierarchies can be cached beyond a request and shared
        // because the hierarchy does not depend on the user.
        // Hierarchies do however change slowly over time.
        //
        // A per-request cache gives immediate reflection of external hierarchy
        // changes (a network request).
        if ( false ) {
            // Example - a more expensive cache choice but can exist across requests
            // (and hence potential better caching efficiency)
            com.github.benmanes.caffeine.cache.Cache<X,Y> cache =
                    Caffeine.newBuilder()
                        .maximumSize(size)
                        .expireAfterWrite(10_000, TimeUnit.MILLISECONDS)
                        //.recordStats()
                        .build();
            return new CacheCaffeine<>(cache);
        }
        // Until proven otherwise, use a lightweight, per request cache that will
        // rebuild each request to immediately reflect hierarchy changes.
        return CacheFactory.createSimpleCache(size);
    }

    public AttributeValueSet requestAttributes() { return requestAttributes; }

    public Hierarchy getHierarchy(Attribute attr) { return attrHierarchy.getHierarchy(attr); }

    public Collection<ValueTerm> getValue(Attribute attribute) {
        return requestAttributes.get(attribute);
    }

    public Cache<Label, ValueTerm> labelEvalCache()  { return evalCache ; }

    public Cache<Attribute, Optional<Hierarchy>> hierarchyCache()  { return hierarchyCache; }

    public DatasetGraph data() { return baseData; }

    public Object requestId() { return id; }

    public void tracking(Track trace) { this.trace = trace; }
    public Track tracking() { return trace; }

    public boolean debug() { return tracking() == Track.DEBUG || tracking() == Track.TRACE; }
    public boolean trace() { return tracking() == Track.TRACE; }
}
