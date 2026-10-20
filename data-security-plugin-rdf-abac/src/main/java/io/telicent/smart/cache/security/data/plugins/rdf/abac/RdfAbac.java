/**
 * Copyright (C) Telicent Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.telicent.smart.cache.security.data.plugins.rdf.abac;

import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RdfAbac {
    /**
     * Environment variable controlling the label parser cache size for the label parsing cache maintained by the
     * singleton instance of the {@link RdfAbacParser}.
     */
    public static final String ENV_PARSER_CACHE_SIZE = "RDF_ABAC_LABEL_PARSER_CACHE_SIZE";
    /**
     * Environment variable controlling the label parser cache duration for the label parsing cache maintained by the
     * singleton instance of the {@link RdfAbacParser}.
     */
    public static final String ENV_PARSER_CACHE_DURATION = "RDF_ABAC_LABEL_PARSER_CACHE_DURATION";
    /**
     * Environment variable controlling the label evaluation cache size for the label evaluation caches that are
     * maintained by each unique instance of the {@link RdfAbacAuthorizer}
     */
    public static final String ENV_LABEL_EVALUATION_CACHE_SIZE = "RDF_ABAC_LABEL_EVALUATION_CACHE_SIZE";
    /**
     * Default size for the label evaluation cache if not configured via {@link #ENV_LABEL_EVALUATION_CACHE_SIZE}
     */
    public static final int DEFAULT_EVALUATION_CACHE_SIZE = 1_000;
    /**
     * Default size for the label parser cache if not configured via {@link #ENV_PARSER_CACHE_SIZE}
     */
    public static final int DEFAULT_PARSER_CACHE_SIZE = 10_000;
    /**
     * Default initial size for the label parser cache, in practise the initial size is the lesser of this or 1/10th of
     * the configured {@link #DEFAULT_PARSER_CACHE_SIZE}
     */
    public static final int DEFAULT_PARSER_CACHE_MIN_SIZE = 1_000;

    static final String CLASSIFICATION = "classification";
    static final String CLEARANCE = "clearance";
    static final Hierarchy
            CLASSIFICATION_HIERARCHY = Hierarchy.create(CLASSIFICATION, "O", "OS", "S", "TS");

    static Hierarchy getClassificationHierarchy(Attribute attribute) {
        if (attribute != null) {
            if (Objects.equals(CLASSIFICATION, attribute.name())) {
                return CLASSIFICATION_HIERARCHY;
            }

            if (Objects.equals(CLEARANCE, attribute.name())) {
                return CLASSIFICATION_HIERARCHY;
            }
        }

        return null;
    }
}
