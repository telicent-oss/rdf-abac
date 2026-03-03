package io.telicent.jena.abac.bulk;

import io.telicent.jena.abac.rocks.StorageFormatProviderUtility;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.Arguments;

import java.util.stream.Stream;

/**
 * Run {@link BulkDirectory} tests using the node-based RocksDB label store,
 * using a setup extension which creates the right kind of store.
 */
@ExtendWith(RocksDBSetupExtension.class)
public class BulkDirectoryRocksDBTestsByNode extends AbstractBulkDirectoryRocksDB {

    public static Stream<Arguments> provideStorageFormat() {
        return StorageFormatProviderUtility.provideStorageFormatsByNode();
    }

}
