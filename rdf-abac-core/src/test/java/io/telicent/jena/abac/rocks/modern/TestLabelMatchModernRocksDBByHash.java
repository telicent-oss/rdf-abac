package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.rocks.StorageFormatProviderUtility;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

public class TestLabelMatchModernRocksDBByHash extends AbstractTestLabelMatchModernRocks {
    /**
     * This method provides the relevant StorageFmtByHash with underlying Hash, combined with LabelMode values
     */
    public static Stream<Arguments> provideStorageFormat() {
        return StorageFormatProviderUtility.provideStorageFormatsByHash();
    }
}