package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.StoreFmt;
import io.telicent.jena.abac.labels.StoreFmtByString;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
import io.telicent.smart.cache.storage.labels.rocksdb.RocksDbLabelsStore;
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

public class LegacyCorrupter extends AbstractRocksDBStorage {
    public LegacyCorrupter(File dbDir) throws RocksDBException, IOException {
        this(dbDir, 10, 50, 1000, 25, null);
    }


    public LegacyCorrupter(File dbDir, int totalCorruptKeys) throws RocksDBException, IOException {
        this(dbDir, totalCorruptKeys, 50, 1000, 25, null);
    }

    public LegacyCorrupter(File dbDir, int numCorruptKeys, int minKeyLength, int maxKeyLength, int valueLength, StoreFmt storeFmt) throws
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
        List<ColumnFamilyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY));
        for (byte[] name : RocksDBHelper.LEGACY_COLUMN_FAMILIES) {
            descriptors.add(new ColumnFamilyDescriptor(name, cfOptions));
        }
        return descriptors;
    }
}
