package io.telicent.jena.abac.rocks;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class TestLegacyLabelsStoreRocksDBByString extends BaseTestLegacyLabelsStoreRocksDB {
    public static Stream<Arguments> provideStorageFormat() {
        return StorageFormatProviderUtility.provideStorageFormatsByString();
    }
}
