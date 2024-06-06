package io.telicent.jena.abac.bulk;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDBException;

public abstract class AbstractBulkDirectoryRocksDB extends BulkDirectory {

    @Test
    public void starWarsReadLoad() throws RocksDBException {

        LoadStats stats = bulkLoadAndRepeatedlyRead(CONTENT_DIR, 0.01, 1000);
        stats.report("starwars files ");
    }

    @Disabled("too big/slow - used for manually checking read capacity")
    @Test
    public void biggerFilesReadLoad() throws RocksDBException {

        var stats = bulkLoadAndRepeatedlyRead(
            directoryProperty("abac.labelstore.biggerfiles").getAbsolutePath(),
            0.001,
            1000);
        stats.report("bigger files ");
    }

    @Disabled("too big/slow - used for manually checking read capacity")
    @Test
    public void biggestFilesReadLoad() throws RocksDBException {

        var stats = bulkLoadAndRepeatedlyRead(
            directoryProperty("abac.labelstore.biggestfiles").getAbsolutePath(),
            0.001,
            100); // use 0.0001,1000 to search fewer keys more often, too few keys may cache ?
        stats.report("biggest files ");
    }
}
