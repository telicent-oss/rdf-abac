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
import org.mockito.Mockito;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestDatasetFilterProvider {

    @AfterEach
    public void resetFilterProvider() {
        ABAC.resetDatasetFilterProvider();
    }

    @Test
    public void test_defaultProvider() {
        assertSame(ABAC.DEFAULT_DATASET_FILTER_PROVIDER, ABAC.getDatasetFilterProvider(),
                   "Initial provider should be the built-in default");
        assertInstanceOf(DefaultDatasetFilterProvider.class, ABAC.getDatasetFilterProvider());
    }

    @Test
    public void test_resetBehaviour() {
        DatasetFilterProvider customFilter = new CountCallsFilterProvider(Mockito.mock(DatasetGraph.class));
        ABAC.setDatasetFilterProvider(customFilter);
        assertSame(customFilter, ABAC.getDatasetFilterProvider(), "Custom provider should be installed");
        assertNotSame(ABAC.DEFAULT_DATASET_FILTER_PROVIDER, ABAC.getDatasetFilterProvider(),
                      "Custom provider must replace the default after set");

        ABAC.resetDatasetFilterProvider();
        assertSame(ABAC.DEFAULT_DATASET_FILTER_PROVIDER, ABAC.getDatasetFilterProvider(),
                   "Reset should restore the default provider");
    }

    @Test
    public void test_nullProvider() {
        assertThrows(NullPointerException.class, () -> ABAC.setDatasetFilterProvider(null),
                     "Null providers must be rejected");
    }

    @Test
    public void test_applyGlobalFilter() {
        DatasetGraph dsg = Mockito.mock(DatasetGraph.class);
        CountCallsFilterProvider provider = new CountCallsFilterProvider(dsg);
        ABAC.setDatasetFilterProvider(provider);

        DatasetGraph dsgBase = Mockito.mock(DatasetGraph.class);
        LabelsStore labelsStore = Mockito.mock(LabelsStore.class);
        CxtABAC cxt = Mockito.mock(CxtABAC.class);

        DatasetGraph result = ABAC.filterDataset(dsgBase, labelsStore, Label.fromText("test"), cxt);

        assertSame(dsg, result, "filterDataset should delegate to the registered provider");
        assertEquals(1, provider.calls.get(), "Custom provider should have been called exactly once");
    }

    @Test
    public void test_applyGlobalFilterAlt() {
        DatasetGraph dsg = Mockito.mock(DatasetGraph.class);
        CountCallsFilterProvider provider = new CountCallsFilterProvider(dsg);
        ABAC.setDatasetFilterProvider(provider);

        DatasetGraphABAC dsgAuth = createDSGABACMock();
        CxtABAC cxt = Mockito.mock(CxtABAC.class);

        DatasetGraph result = ABAC.filterDataset(dsgAuth, cxt);

        assertSame(dsg, result, "filterDataset should delegate to the registered provider");
        assertEquals(1, provider.calls.get(), "Custom provider should have been called exactly once");
    }

    @Test
    public void test_applyDataSetFilter() {
        DatasetGraph globalDSG = Mockito.mock(DatasetGraph.class);
        DatasetGraph localDSG = Mockito.mock(DatasetGraph.class);
        CountCallsFilterProvider globalProvider = new CountCallsFilterProvider(globalDSG);
        CountCallsFilterProvider datasetProvider = new CountCallsFilterProvider(localDSG);

        ABAC.setDatasetFilterProvider(globalProvider);

        DatasetGraphABAC dsgAuth = createDSGABACMock();
        dsgAuth.setFilterProvider(datasetProvider);

        DatasetGraph result = ABAC.filterDataset(dsgAuth, Mockito.mock(CxtABAC.class));

        assertSame(localDSG, result,
                   "Per-dataset provider should take precedence over the global provider");
        assertEquals(1, datasetProvider.calls.get(),
                     "Per-dataset provider should have been invoked exactly once");
        assertEquals(0, globalProvider.calls.get(),
                     "Global provider should not have been invoked when a per-dataset provider is set");
    }

    @Test
    public void test_datasetFilter_fallsBackToGlobal() {
        DatasetGraph globalDSG = Mockito.mock(DatasetGraph.class);
        DatasetGraph localDSG = Mockito.mock(DatasetGraph.class);
        CountCallsFilterProvider globalProvider = new CountCallsFilterProvider(globalDSG);
        CountCallsFilterProvider perDatasetProvider = new CountCallsFilterProvider(localDSG);

        ABAC.setDatasetFilterProvider(globalProvider);

        DatasetGraphABAC dsgAuth = createDSGABACMock();
        dsgAuth.setFilterProvider(perDatasetProvider);
        // Clear the override:
        dsgAuth.setFilterProvider(null);

        DatasetGraph result = ABAC.filterDataset(dsgAuth, Mockito.mock(CxtABAC.class));

        assertSame(globalDSG, result,
                   "After clearing the per-dataset override, the global provider should be used");
        assertEquals(0, perDatasetProvider.calls.get(),
                     "Cleared per-dataset provider should not have been invoked");
        assertEquals(1, globalProvider.calls.get(),
                     "Global provider should be invoked when no per-dataset override is set");
    }

    private static DatasetGraphABAC createDSGABACMock() {
        return new DatasetGraphABAC(Mockito.mock(DatasetGraph.class),
                                    "attr=1",
                                    Mockito.mock(LabelsStore.class),
                                    Label.fromText("test"),
                                    Mockito.mock(AttributesStore.class));
    }

    private static final class CountCallsFilterProvider implements DatasetFilterProvider {
        private final DatasetGraph datasetGraph;
        final AtomicInteger calls = new AtomicInteger();

        CountCallsFilterProvider(DatasetGraph datasetGraph) {
            this.datasetGraph = datasetGraph;
        }

        @Override
        public DatasetGraph filterDataset(DatasetGraphABAC dsgAuth, CxtABAC cxt) {
            calls.incrementAndGet();
            return datasetGraph;
        }

        @Override
        public DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels, Label defaultLabel,
                                          CxtABAC cxt) {
            calls.incrementAndGet();
            return datasetGraph;
        }
    }
}
