package io.telicent.jena.abac.rocks;

import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class TestLegacyLabelsStoreRocksDBByHash extends BaseTestLegacyLabelsStoreRocksDB {
    /**
     * This method provides the relevant StorageFmtByHash with underlying Hash, combined with LabelMode values
     */
    public static Stream<Arguments> provideStorageFormat() {
        return StorageFormatProviderUtility.provideStorageFormatsByHash();
    }
}
