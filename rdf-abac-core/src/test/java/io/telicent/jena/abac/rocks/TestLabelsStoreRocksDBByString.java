package io.telicent.jena.abac.rocks;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class TestLabelsStoreRocksDBByString extends BaseTestLabelsStoreRocksDB {
    public static Stream<Arguments> provideLabelAndStorageFmt() {
        return LabelAndStorageFormatProviderUtility.provideLabelAndStorageFmtByString();
    }
}
