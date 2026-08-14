package io.telicent.jena.abac;

import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.shacl.Shapes;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphZero;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class TestABAC {

    @Test
    void abac_is_dataset_abac_true() {
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = mock(AttributesStore.class);
        DatasetGraph datasetGraph = new DatasetGraphABAC(mockDatasetGraph, "attr=1", mockLabelsStore, Label.fromText("test"), mockedAttributesStore);
        assertTrue(ABAC.isDatasetABAC(datasetGraph));
    }

    @Test
    void abac_is_dataset_abac_false() {
        assertFalse(ABAC.isDatasetABAC(DatasetGraphZero.create()));
    }

    @Test
    void abac_request_dataset() {
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = mock(AttributesStore.class);
        DatasetGraphABAC datasetGraph = new DatasetGraphABAC(mockDatasetGraph, "attr=1", mockLabelsStore, Label.fromText("test"), mockedAttributesStore);
        DatasetGraph dsg = ABAC.requestDataset(datasetGraph, AttributeValueSet.of("test"), mockedAttributesStore);
        assertNotNull(dsg);
    }

    @Test
    void abac_authz_dataset() {
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = mock(LabelsStore.class);
        AttributesStore mockedAttributesStore = mock(AttributesStore.class);
        DatasetGraphABAC datasetGraph = new DatasetGraphABAC(mockDatasetGraph, "attr=1", mockLabelsStore, Label.fromText("test"), mockedAttributesStore);
        DatasetGraph dsg = ABAC.authzDataset(datasetGraph, mockLabelsStore, Label.fromText("test"), mockedAttributesStore);
        assertNotNull(dsg);
    }

    @Test
    void abac_filter_dataset_01() {
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        LabelsStore mockLabelsStore = mock(LabelsStore.class);
        CxtABAC mockContext = mock(CxtABAC.class);
        DatasetGraph dsg = ABAC.filterDataset(mockDatasetGraph, mockLabelsStore, Label.fromText("test"), mockContext);
        assertNotNull(dsg);
    }

    @Test
    void abac_filter_dataset_02() {
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC mockContext = mock(CxtABAC.class);
        DatasetGraph dsg = ABAC.filterDataset(mockDatasetGraph, null, Label.fromText("test"), mockContext);
        assertNotNull(dsg);
    }

    @Test
    void abac_read_shacl() {
        Shapes shapes = ABAC.readSHACL("TestShape.ttl");
        assertNotNull(shapes);
    }
}
