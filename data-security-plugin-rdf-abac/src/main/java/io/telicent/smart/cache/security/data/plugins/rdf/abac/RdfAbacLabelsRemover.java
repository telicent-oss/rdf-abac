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

import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.security.data.DataSecurityException;
import io.telicent.smart.cache.security.data.labels.SecurityLabelsRemover;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;

/**
 * @deprecated As of release 1.7.4, replaced by {@link RdfAbacDatasetGraphLabelled#removeLabels(Quad)} .}
 */
@Deprecated(since = "1.7.4", forRemoval = true)
public class RdfAbacLabelsRemover implements SecurityLabelsRemover {

    @Override
    public void remove(DatasetGraph datasetGraph, Quad quad) throws DataSecurityException {
        if(datasetGraph instanceof DatasetGraphABAC datasetGraphABAC) {
            // The labels store is owned by the DatasetGraphABAC and must stay open after removal
            final LabelsStore labels = datasetGraphABAC.labelsStore();
            try{
                labels.remove(quad);
            } catch (Exception e){
                throw new DataSecurityException(e.getMessage(),e);
            }
        }
    }

}
