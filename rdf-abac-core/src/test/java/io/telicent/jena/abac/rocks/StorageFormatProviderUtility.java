package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.labels.*;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.telicent.jena.abac.labels.hashing.HasherUtil.hasherMap;

public class StorageFormatProviderUtility {
    private StorageFormatProviderUtility() {
    }

    /**
     * This method provides a StorageFmtByString, combined with LabelMode values
     */
    public static Stream<Arguments> provideStorageFormatsByString() {
        return Stream.of(new StoreFmtByString()).map(Arguments::of);
    }

    /**
     * This method provides the relevant StorageFmtByHash with underlying Hash, combined with LabelMode values
     */
    public static Stream<Arguments> provideStorageFormatsByHash() {
        // Get a stream of Hashers from the hasherMap
        return hasherMap.values()
                        .stream()
                        .map(Supplier::get)
                        .map(StoreFmtByHash::new)
                        .map(Arguments::of);  // Get each Hasher from the Supplier


    }
}
