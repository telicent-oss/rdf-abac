package io.telicent.jena.abac.bulk;

import io.telicent.jena.abac.labels.NaiveNodeTable;
import io.telicent.jena.abac.labels.StoreFmtByNodeId;
import io.telicent.jena.abac.labels.TrieNodeTable;
import io.telicent.jena.abac.rocks.LabelAndStorageFormatProviderUtility;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Run {@link BulkDirectory} tests using the node-based RocksDB label store,
 * using a setup extension which creates the right kind of store.
 */
@ExtendWith(RocksDBSetupExtension.class)
public class BulkDirectoryRocksDBTestsByNode extends AbstractBulkDirectoryRocksDB {

    /**
     * This method provides the relevant Node Tables
     */
    protected static Stream<Arguments> provideStorageFmt() {
        return Stream.of(
                Arguments.of(new StoreFmtByNodeId(new NaiveNodeTable())),
                Arguments.of(new StoreFmtByNodeId(new TrieNodeTable()))
        );
    }

    /**
     * This method provides the relevant StorageFmtByNode with underlying Node Tables, combined with LabelMode values
     */
    public static Stream<Arguments> provideLabelAndStorageFmt() {
        return LabelAndStorageFormatProviderUtility.provideLabelAndStorageFmtByNode();
    }

}
