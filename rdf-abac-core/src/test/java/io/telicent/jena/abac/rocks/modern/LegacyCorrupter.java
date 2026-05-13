package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.StoreFmt;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
import io.telicent.smart.cache.storage.rocksdb.AbstractRocksDBStorage;
import io.telicent.smart.cache.storage.rocksdb.TransactionContext;
import org.apache.commons.lang3.RandomUtils;
import org.rocksdb.ColumnFamilyDescriptor;
import org.rocksdb.ColumnFamilyOptions;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * This class allows opening a legacy format store and introducing some number of randomised corrupt key value pairs
 * into the label store.  This is used by {@link TestLabelStoreMigration} to test error handling paths in the automated
 * migration logic implemented by the code in
 * {@link io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB}.
 * <p>
 * Constructor parameters allow controlling how many corrupt key value pairs are introduced into the label store and the
 * sizes of those key value pairs.
 * </p>
 */
public class LegacyCorrupter extends AbstractRocksDBStorage {

    /**
     * Opens the legacy labels store in the given directory and inserts the desired number of corrupt key value pairs
     *
     * @param dbDir            Database directory
     * @param totalCorruptKeys Number of corrupt key value pairs to insert
     * @throws RocksDBException Thrown if the database cannot be opened
     * @throws IOException      Thrown if the database cannot be opened
     */
    public LegacyCorrupter(File dbDir, int totalCorruptKeys) throws RocksDBException, IOException {
        this(dbDir, totalCorruptKeys, 50, 1000, 25, null);
    }

    /**
     * Opens the legacy labels store in the given directory and inserts the desired number of corrupt key value pairs
     * with generated keys and values matching the parameters given
     *
     * @param dbDir          Database directory
     * @param numCorruptKeys Number of corrupt key value pairs to insert
     * @param minKeyLength   Minimum (inclusive) corrupt key length to generate
     * @param maxKeyLength   Maximum (exclusive) corrupt key length to generate
     * @param valueLength    Fixed corrupt value length to generate
     * @param storeFmt       An optional store format to record in the database, allows testing the different migration
     *                       code paths for different legacy store formats
     * @throws IOException      Thrown if the database cannot be opened
     * @throws RocksDBException Thrown if the database cannot be opened
     */
    public LegacyCorrupter(File dbDir, int numCorruptKeys, int minKeyLength, int maxKeyLength, int valueLength,
                           StoreFmt storeFmt) throws
            IOException,
            RocksDBException {
        super(dbDir);

        // Add some random corruption to the legacy column family
        try (TransactionContext context = this.begin()) {
            // Record a StoreFmt (if set)
            if (storeFmt != null) {
                context.put(this.getDefaultHandle(), RocksDBHelper.STORE_FORMAT_KEY, storeFmt.toString().getBytes(
                        StandardCharsets.UTF_8));
            }

            // Inject the desired number of corrupted key value pairs
            for (int i = 1; i <= numCorruptKeys; i++) {
                context.put(this.getHandle(RocksDBHelper.COLUMN_FAMILY_SPO),
                            RandomUtils.insecure()
                                       .randomBytes(RandomUtils.insecure().randomInt(minKeyLength, maxKeyLength)),
                            RandomUtils.insecure().randomBytes(valueLength));
            }
            context.commit();
        }
    }

    @Override
    protected List<ColumnFamilyDescriptor> prepareColumnFamilyDescriptors(ColumnFamilyOptions cfOptions) {
        // Returns all the column families the legacy store used
        List<ColumnFamilyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));
        for (byte[] name : RocksDBHelper.LEGACY_COLUMN_FAMILIES) {
            descriptors.add(new ColumnFamilyDescriptor(name, cfOptions));
        }
        return descriptors;
    }
}
