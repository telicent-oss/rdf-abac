package io.telicent.jena.abac;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;

public class TestTransactionalMemory extends AbstractionTransactionalTests {
    @Override
    protected LabelsStore create() {
        return Labels.createLabelsStoreMem();
    }
}
