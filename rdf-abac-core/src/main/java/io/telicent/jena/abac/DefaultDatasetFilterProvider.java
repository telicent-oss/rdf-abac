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
import io.telicent.jena.abac.core.QuadFilter;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsGetter;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.storage.rdf.DatasetGraphFilteredUnionView;
import org.apache.jena.sparql.core.DatasetGraph;

/**
 * A Dataset Filter Provider that preserves the existing historical RDF-ABAC behaviour
 * exposing all named graphs of the underlying dataset, to be applied by default.
 */
public class DefaultDatasetFilterProvider implements DatasetFilterProvider {

    @Override
    public DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
        return filterDataset(dsgAuthz.getData(), dsgAuthz.labelsStore(), dsgAuthz.getDefaultLabel(), cxt);
    }

    @Override
    public DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, Label defaultLabel, CxtABAC cxt) {
        QuadFilter filter = null;
        if (labels != null) {
            LabelsGetter getter = labels::labelForQuad;
            filter = Labels.securityFilterByLabel(getter, defaultLabel, cxt);
        }
        // IMPORTANT - Must use our DatasetGraphFilteredUnionView here not the base Jena DatasetGraphFilteredView. This
        //             is because depending on the additional layers of DatasetGraph wrapping that may be placed around
        //             this return value queries can route through the DatasetGraph API differently.  It turns out the
        //             Jena DatasetGraphFilteredView doesn't handle union default graph mode properly on some code
        //             routes.  Our class overrides the base class and addresses that deficiency.
        return new DatasetGraphFilteredUnionView(dsgBase, filter, new AllNamedGraphs(dsgBase));
    }
}
