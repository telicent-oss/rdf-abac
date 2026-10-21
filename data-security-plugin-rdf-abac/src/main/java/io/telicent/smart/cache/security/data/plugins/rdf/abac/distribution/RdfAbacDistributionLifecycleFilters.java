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
package io.telicent.smart.cache.security.data.plugins.rdf.abac.distribution;

import io.telicent.jena.abac.DatasetFilterProvider;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.smart.cache.security.data.distribution.DistributionLifecycleFilters;
import io.telicent.smart.cache.security.data.distribution.DistributionLifecycleStateFile;
import org.apache.jena.sparql.core.DatasetGraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/** Installs the lifecycle-aware dataset filter when the feature is configured. */
public class RdfAbacDistributionLifecycleFilters implements DistributionLifecycleFilters {

    private static final Logger LOGGER = LoggerFactory.getLogger(RdfAbacDistributionLifecycleFilters.class);

    @Override
    public boolean installIfConfigured(DatasetGraph datasetGraph, String applicationId, String stateFile) {
        if(datasetGraph instanceof DatasetGraphABAC datasetGraphABAC) {
            if (datasetGraphABAC.getFilterProvider() instanceof DistributionLifecycleDatasetFilterProvider) {
                LOGGER.info("Lifecycle-aware dataset filter already installed for this dataset; skipping");
                return false;
            }

            final DatasetFilterProvider delegate = datasetGraphABAC.getFilterProvider();
            datasetGraphABAC.setFilterProvider(new DistributionLifecycleDatasetFilterProvider(
                    new DistributionLifecycleStateFile(Path.of(stateFile), applicationId), delegate));
            if (delegate != null) {
                LOGGER.info("Installed lifecycle-aware dataset filter for RDF ABAC named-graph routing by wrapping existing dataset filter provider");
            } else {
                LOGGER.info("Installed lifecycle-aware dataset filter for RDF ABAC named-graph routing");
            }
            return true;
        } else {
            LOGGER.warn("Dataset graph is not an instance of DatasetGraphABAC");
            return false;
        }
    }

}
