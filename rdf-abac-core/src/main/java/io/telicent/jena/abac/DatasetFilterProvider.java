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

import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.sparql.core.DatasetGraph;

/**
 * Extension point for deciding how an ABAC-protected request dataset is constructed.
 */
public interface DatasetFilterProvider {

    /**
     * Build the filtered dataset for a request against an ABAC-protected dataset.
     *
     * @param dsgAuth The ABAC dataset wrapper containing the data, labels store, and default label.
     * @param cxt      The ABAC evaluation context (user attributes, hierarchy lookup, tracking).
     * @return A Dataset Graph that represents what the request is allowed to see.
     */
    DatasetGraph filterDataset(DatasetGraphABAC dsgAuth, CxtABAC cxt);

    /**
     * Build the filtered dataset for a request against an ABAC-protected dataset.
     *
     * @param dsgBase      The underlying dataset.
     * @param labels       The labels store, or {@code null} to disable label filtering.
     * @param defaultLabel The default label to be applied when none is provided.
     * @param cxt          The ABAC evaluation context.
     * @return A Dataset Graph that represents what the request is allowed to see.
     */
    DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, Label defaultLabel, CxtABAC cxt);
}