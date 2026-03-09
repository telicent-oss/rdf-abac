package io.telicent.jena.abac.rocks;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public abstract class TestLegacyLabelsStoreRocksDBByNodeId extends BaseTestLegacyLabelsStoreRocksDB {
    public static Stream<Arguments> provideStorageFormat() {
        return StorageFormatProviderUtility.provideStorageFormatsByNode();
    }
}
