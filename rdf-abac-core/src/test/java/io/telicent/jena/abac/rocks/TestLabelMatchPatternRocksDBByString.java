package io.telicent.jena.abac.rocks;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class TestLabelMatchPatternRocksDBByString extends BaseTestLabelMatchPatternRocksDB {
    public static Stream<Arguments> provideLabelAndStorageFmt() {
        return LabelAndStorageFormatProviderUtility.provideLabelAndStorageFmtByString();
    }
}
