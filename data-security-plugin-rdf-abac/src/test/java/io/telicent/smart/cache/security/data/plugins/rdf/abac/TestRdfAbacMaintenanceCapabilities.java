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
import io.telicent.smart.cache.storage.BackupRestoreCapable;
import io.telicent.smart.cache.storage.CompactCapable;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.testng.Assert;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

public class TestRdfAbacMaintenanceCapabilities {
    @Test
    public void capabilitiesRemainOwnedByDataset() throws Exception {
        LabelsStore store = mock(LabelsStore.class, withSettings().extraInterfaces(BackupRestoreCapable.class, CompactCapable.class));
        DatasetGraphABAC dataset = mock(DatasetGraphABAC.class);
        when(dataset.labelsStore()).thenReturn(store);
        RdfAbacPlugin plugin = new RdfAbacPlugin();
        Assert.assertSame(plugin.prepareLabelsBackup(dataset).orElseThrow(), store); //NOSONAR
        Assert.assertSame(plugin.prepareLabelsRestore(dataset).orElseThrow(), store); //NOSONAR
        Assert.assertSame(plugin.prepareLabelsCompact(dataset).orElseThrow(), store); //NOSONAR
        verify(store, never()).close();
        verifyNoInteractions(store);
    }

    @Test
    public void unsupportedDatasetsHaveNoMaintenanceCapability() {
        RdfAbacPlugin plugin = new RdfAbacPlugin();
        Assert.assertTrue(plugin.prepareLabelsBackup(null).isEmpty()); //NOSONAR
        Assert.assertTrue(plugin.prepareLabelsRestore(DatasetGraphFactory.createTxnMem()).isEmpty()); //NOSONAR
        DatasetGraphABAC dataset = mock(DatasetGraphABAC.class);
        when(dataset.labelsStore()).thenReturn(mock(LabelsStore.class));
        Assert.assertTrue(plugin.prepareLabelsBackup(dataset).isEmpty()); //NOSONAR
        Assert.assertTrue(plugin.prepareLabelsRestore(dataset).isEmpty()); //NOSONAR
        Assert.assertTrue(plugin.prepareLabelsCompact(dataset).isEmpty()); //NOSONAR
    }
}
