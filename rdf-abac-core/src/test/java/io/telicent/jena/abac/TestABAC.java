package io.telicent.jena.abac;

import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class TestABAC {

    @Test
    public void abac_is_dataset_abac_true() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = Mockito.mock(AttributesStore.class);
        DatasetGraph datasetGraph = new DatasetGraphABAC(mockDatasetGraph, "attr=1", mockLabelsStore, "test", mockedAttributesStore);
        assertTrue(ABAC.isDatasetABAC(datasetGraph));
    }

    @Test
    public void abac_is_dataset_abac_false() {
        assertFalse(ABAC.isDatasetABAC(DatasetGraphZero.create()));
    }

    @Test
    public void abac_request_dataset() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = Mockito.mock(AttributesStore.class);
        DatasetGraphABAC datasetGraph = new DatasetGraphABAC(mockDatasetGraph, "attr=1", mockLabelsStore, "test", mockedAttributesStore);
        DatasetGraph dsg = ABAC.requestDataset(datasetGraph, AttributeValueSet.of("test"), mockedAttributesStore);
        assertNotNull(dsg);
    }

    @Test
    public void abac_authz_dataset() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = Mockito.mock(AttributesStore.class);
        DatasetGraphABAC datasetGraph = new DatasetGraphABAC(mockDatasetGraph, "attr=1", mockLabelsStore, "test", mockedAttributesStore);
        DatasetGraph dsg = ABAC.authzDataset(datasetGraph, mockLabelsStore, "test", mockedAttributesStore);
        assertNotNull(dsg);
    }

    @Test
    public void abac_filter_dataset_01() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = Mockito.mock(LabelsStore.class);
        CxtABAC mockContext = Mockito.mock(CxtABAC.class);
        DatasetGraph dsg = ABAC.filterDataset(mockDatasetGraph, mockLabelsStore, "test", mockContext);
        assertNotNull(dsg);
    }

    @Test
    public void abac_filter_dataset_02() {
        DatasetGraph mockDatasetGraph = Mockito.mock(DatasetGraph.class);
        CxtABAC mockContext = Mockito.mock(CxtABAC.class);
        DatasetGraph dsg = ABAC.filterDataset(mockDatasetGraph, null, "test", mockContext);
        assertNotNull(dsg);
    }

    @Test
    public void abac_read_shacl() {
        Shapes shapes = ABAC.readSHACL("TestShape.ttl");
        assertNotNull(shapes);
    }
}
