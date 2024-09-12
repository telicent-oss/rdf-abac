package io.telicent.jena.abac.bulk;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.LabelsStoreRocksDB;
import io.telicent.jena.abac.labels.StoreFmt;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.util.stream.Stream;

public abstract class AbstractBulkDirectoryRocksDB extends BulkDirectory {

    @Override
    LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws RocksDBException {
        return Labels.createLabelsStoreRocksDB(dbDir, labelMode, null, storeFmt);
    }

    static Stream<Arguments> provideStorageFmt() {
        return Stream.of();
    }

    @ParameterizedTest
    @MethodSource("provideStorageFmt")
    public void starWarsReadLoad(StoreFmt storeFmt) throws RocksDBException {

        LoadStats stats = bulkLoadAndRepeatedlyRead(CONTENT_DIR, storeFmt,0.01, 1000);
        stats.report("starwars files ");
    }

    @Disabled("too big/slow - used for manually checking read capacity")
    @ParameterizedTest
    @MethodSource("provideStorageFmt")
    public void biggerFilesReadLoad(StoreFmt storeFmt) throws RocksDBException {

        var stats = bulkLoadAndRepeatedlyRead(
            directoryProperty("abac.labelstore.biggerfiles").getAbsolutePath(),
            storeFmt,
            0.001,
            1000);
        stats.report("bigger files ");
    }

    @Disabled("too big/slow - used for manually checking read capacity")
    @ParameterizedTest
    @MethodSource("provideStorageFmt")
    public void biggestFilesReadLoad(StoreFmt storeFmt) throws RocksDBException {

        var stats = bulkLoadAndRepeatedlyRead(
            directoryProperty("abac.labelstore.biggestfiles").getAbsolutePath(),
            storeFmt,
            0.001,
            100); // use 0.0001,1000 to search fewer keys more often, too few keys may cache ?
        stats.report("biggest files ");
    }
}
