package io.telicent.jena.abac.rocks;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class TestLabelMatchPatternRocksDBByNodeId extends BaseTestLabelMatchPatternRocksDB {
    public static Stream<Arguments> provideLabelAndStorageFmt() {
        return LabelAndStorageFormatProviderUtility.provideLabelAndStorageFmtByNode();
    }
}
