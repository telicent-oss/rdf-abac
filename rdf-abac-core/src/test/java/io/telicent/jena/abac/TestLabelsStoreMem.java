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

import io.telicent.jena.abac.labels.L;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreMem;
import io.telicent.jena.abac.rocks.AbstractTestLabelsStoreRocksDB;
import org.apache.jena.graph.Graph;

/**
 * Test storing labels.
 * These are the general contract tests for a {@link LabelsStore}.
 * <p>
 * See {@link TestLabelsStoreMem} for {@link Labels#createLabelsStoreMem}
 * (i.e.  {@link LabelsStoreMem}) testing using {@link AbstractTestLabelsStoreRocksDB}.
 * <p>
 */
public class TestLabelsStoreMem extends AbstractTestLabelsStore {

    @Override
    protected LabelsStore createLabelsStore() {
        return Labels.createLabelsStoreMem();
    }

    @Override
    protected LabelsStore createLabelsStore(Graph input) {
        LabelsStore labelStore = createLabelsStore();
        L.loadStoreFromGraph(labelStore, input);
        return labelStore;
    }
}
