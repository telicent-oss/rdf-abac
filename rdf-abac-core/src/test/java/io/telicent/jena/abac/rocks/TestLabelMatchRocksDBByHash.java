package io.telicent.jena.abac.rocks;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class TestLabelMatchRocksDBByHash extends AbstractTestLabelMatchRocks {
    /**
     * This method provides the relevant StorageFmtByHash with underlying Hash, combined with LabelMode values
     */
    public static Stream<Arguments> provideLabelAndStorageFmt() {
        return LabelAndStorageFormatProviderUtility.provideLabelAndStorageFmtByHash();
    }
}