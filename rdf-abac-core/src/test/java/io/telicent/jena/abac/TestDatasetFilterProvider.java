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

import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.sparql.core.DatasetGraph;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class TestDatasetFilterProvider {

    @AfterEach
    void resetFilterProvider() {
        ABAC.resetDatasetFilterProvider();
    }

    @Test
    void test_defaultProvider() {
        assertSame(ABAC.DEFAULT_DATASET_FILTER_PROVIDER, ABAC.getDatasetFilterProvider(),
                   "Initial provider should be the built-in default");
        assertInstanceOf(DefaultDatasetFilterProvider.class, ABAC.getDatasetFilterProvider());
    }

    @Test
    void test_resetBehaviour() {
        DatasetFilterProvider customFilter = new CountCallsFilterProvider(mock(DatasetGraph.class));
        ABAC.setDatasetFilterProvider(customFilter);
        assertSame(customFilter, ABAC.getDatasetFilterProvider(), "Custom provider should be installed");
        assertNotSame(ABAC.DEFAULT_DATASET_FILTER_PROVIDER, ABAC.getDatasetFilterProvider(),
                      "Custom provider must replace the default after set");

        ABAC.resetDatasetFilterProvider();
        assertSame(ABAC.DEFAULT_DATASET_FILTER_PROVIDER, ABAC.getDatasetFilterProvider(),
                   "Reset should restore the default provider");
    }

    @Test
    void test_nullProvider() {
        assertThrows(NullPointerException.class, () -> ABAC.setDatasetFilterProvider(null),
                     "Null providers must be rejected");
    }

    @Test
    void test_applyGlobalFilter() {
        DatasetGraph dsg = mock(DatasetGraph.class);
        CountCallsFilterProvider provider = new CountCallsFilterProvider(dsg);
        ABAC.setDatasetFilterProvider(provider);

        DatasetGraph dsgBase = mock(DatasetGraph.class);
        LabelsStore labelsStore = mock(LabelsStore.class);
        CxtABAC cxt = mock(CxtABAC.class);

        DatasetGraph result = ABAC.filterDataset(dsgBase, labelsStore, Label.fromText("test"), cxt);

        assertSame(dsg, result, "filterDataset should delegate to the registered provider");
        assertEquals(1, provider.calls.get(), "Custom provider should have been called exactly once");
    }

    @Test
    void test_applyGlobalFilterAlt() {
        DatasetGraph dsg = mock(DatasetGraph.class);
        CountCallsFilterProvider provider = new CountCallsFilterProvider(dsg);
        ABAC.setDatasetFilterProvider(provider);

        DatasetGraphABAC dsgAuth = createDSGABACMock();
        CxtABAC cxt = mock(CxtABAC.class);

        DatasetGraph result = ABAC.filterDataset(dsgAuth, cxt);

        assertSame(dsg, result, "filterDataset should delegate to the registered provider");
        assertEquals(1, provider.calls.get(), "Custom provider should have been called exactly once");
    }

    @Test
    void test_applyDataSetFilter() {
        DatasetGraph globalDSG = mock(DatasetGraph.class);
        DatasetGraph localDSG = mock(DatasetGraph.class);
        CountCallsFilterProvider globalProvider = new CountCallsFilterProvider(globalDSG);
        CountCallsFilterProvider datasetProvider = new CountCallsFilterProvider(localDSG);

        ABAC.setDatasetFilterProvider(globalProvider);

        DatasetGraphABAC dsgAuth = createDSGABACMock();
        dsgAuth.setFilterProvider(datasetProvider);

        DatasetGraph result = ABAC.filterDataset(dsgAuth, mock(CxtABAC.class));

        assertSame(localDSG, result,
                   "Per-dataset provider should take precedence over the global provider");
        assertEquals(1, datasetProvider.calls.get(),
                     "Per-dataset provider should have been invoked exactly once");
        assertEquals(0, globalProvider.calls.get(),
                     "Global provider should not have been invoked when a per-dataset provider is set");
    }

    @Test
    void test_datasetFilter_fallsBackToGlobal() {
        DatasetGraph globalDSG = mock(DatasetGraph.class);
        DatasetGraph localDSG = mock(DatasetGraph.class);
        CountCallsFilterProvider globalProvider = new CountCallsFilterProvider(globalDSG);
        CountCallsFilterProvider perDatasetProvider = new CountCallsFilterProvider(localDSG);

        ABAC.setDatasetFilterProvider(globalProvider);

        DatasetGraphABAC dsgAuth = createDSGABACMock();
        dsgAuth.setFilterProvider(perDatasetProvider);
        // Clear the override:
        dsgAuth.setFilterProvider(null);

        DatasetGraph result = ABAC.filterDataset(dsgAuth, mock(CxtABAC.class));

        assertSame(globalDSG, result,
                   "After clearing the per-dataset override, the global provider should be used");
        assertEquals(0, perDatasetProvider.calls.get(),
                     "Cleared per-dataset provider should not have been invoked");
        assertEquals(1, globalProvider.calls.get(),
                     "Global provider should be invoked when no per-dataset override is set");
    }

    private static DatasetGraphABAC createDSGABACMock() {
        return new DatasetGraphABAC(mock(DatasetGraph.class),
                                    "attr=1",
                                    mock(LabelsStore.class),
                                    Label.fromText("test"),
                                    mock(AttributesStore.class));
    }

    private static final class CountCallsFilterProvider implements DatasetFilterProvider {
        private final DatasetGraph datasetGraph;
        final AtomicInteger calls = new AtomicInteger();

        CountCallsFilterProvider(DatasetGraph datasetGraph) {
            this.datasetGraph = datasetGraph;
        }

        @Override
        DatasetGraph filterDataset(DatasetGraphABAC dsgAuth, CxtABAC cxt) {
            calls.incrementAndGet();
            return datasetGraph;
        }

        @Override
        DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, Label defaultLabel,
                                          CxtABAC cxt) {
            calls.incrementAndGet();
            return datasetGraph;
        }
    }
}
