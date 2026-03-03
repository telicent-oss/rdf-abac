package io.telicent.jena.abac.rocks;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public abstract class TestLabelsStoreRocksDBByNodeId extends BaseTestLabelsStoreRocksDB {
    public static Stream<Arguments> provideStorageFormat() {
        return StorageFormatProviderUtility.provideStorageFormatsByNode();
    }
}
