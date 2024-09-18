package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.labels.*;
import io.telicent.jena.abac.labels.hashing.Hasher;
import org.junit.jupiter.params.provider.Arguments;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.telicent.jena.abac.labels.hashing.HasherUtil.hasherMap;

public class LabelAndStorageFormatProviderUtility {
    private LabelAndStorageFormatProviderUtility() {}
    /**
     * This method provides the relevant StorageFmtByNode with underlying Node Tables, combined with LabelMode values
     */
    public static Stream<Arguments> provideLabelAndStorageFmtByNode() {
        Stream<StoreFmtByNodeId> tableStream = Stream.of(
                new StoreFmtByNodeId(new NaiveNodeTable()),
                new StoreFmtByNodeId(new TrieNodeTable())
        );
        return tableStream.flatMap(nodeTable ->
                Stream.of(LabelsStoreRocksDB.LabelMode.values())   // Stream of LabelMode values
                        .map(labelMode -> Arguments.of(labelMode, nodeTable)) // Combine NodeTable with LabelMode
        );
    }

    /**
     * This method provides a StorageFmtByString, combined with LabelMode values
     */
    public static Stream<Arguments> provideLabelAndStorageFmtByString() {
        Stream<StoreFmtByString> stream = Stream.of(new StoreFmtByString());
        return stream.flatMap(storeFmtByString ->
                Stream.of(LabelsStoreRocksDB.LabelMode.values())   // Stream of LabelMode values
                        .map(labelMode -> Arguments.of(labelMode, storeFmtByString)) // Combine Store by String with LabelMode
        );
    }

    /**
     * This method provides the relevant StorageFmtByHash with underlying Hash, combined with LabelMode values
     */
    public static Stream<Arguments> provideLabelAndStorageFmtByHash() {
        // Get a stream of Hashers from the hasherMap
        Stream<Hasher> hasherStream = hasherMap.values().stream()
                .map(Supplier::get);  // Get each Hasher from the Supplier

        // Combine each LabelMode with each Hasher and create a StorageFmtHash
        return hasherStream.flatMap(hasher ->
                Stream.of(LabelsStoreRocksDB.LabelMode.values())   // Stream of LabelMode values
                        .map(labelMode -> {
                            // Create a new instance of StorageFmtHash with the hasher
                            StoreFmtByHash storageFmtHash = new StoreFmtByHash(hasher);
                            // Return both the LabelMode and the StorageFmtHash as arguments
                            return Arguments.of(labelMode, storageFmtHash);
                        })
        );

    }
}
