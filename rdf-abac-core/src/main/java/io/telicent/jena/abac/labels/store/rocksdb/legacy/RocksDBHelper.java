package io.telicent.jena.abac.labels.store.rocksdb.legacy;


import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.StoreFmt;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.tdb2.sys.NormalizeTermsTDB2;
import org.rocksdb.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static io.telicent.jena.abac.labels.Labels.LOG;
import static org.apache.jena.sparql.util.NodeUtils.nullToAny;

public class RocksDBHelper {

    /**
     * Key stored in the default column family to record the store format used so subsequent store opens can verify that
     * the configured store format matches that previously used
     */
    public static final byte[] STORE_FORMAT_KEY = StoreFmt.class.getSimpleName().getBytes(StandardCharsets.UTF_8);
    private final static String CREATE_MESSAGE = "creation of RocksDB label store";
    /**
     * Name of the column family used to store the triple to label mapping in the {@link LegacyLabelsStoreRocksDB}
     * implementation
     */
    public static final byte[] COLUMN_FAMILY_SPO = "CF_ABAC_SPO".getBytes(StandardCharsets.UTF_8);

    /**
     * Name of the deprecated column family that was historically used to store subject based wildcard pattern matches,
     * retained purely to allow migrating from legacy stores as RocksDB requires us to open all pre-existing column
     * families
     */
    @Deprecated
    public static final byte[] COLUMN_FAMILY_S = "CF_ABAC_S**".getBytes(StandardCharsets.UTF_8);

    /**
     * Name of the deprecated column family that was historically used to store predicate based wildcard pattern
     * matches, retained purely to allow migrating from legacy stores as RocksDB requires us to open all pre-existing
     * column families
     */
    @Deprecated
    public static final byte[] COLUMN_FAMILY_P = "CF_ABAC_*P*".getBytes(StandardCharsets.UTF_8);

    /**
     * Name of the deprecated column family that was historically used to store wildcard pattern matches, retained
     * purely to allow migrating from legacy stores as RocksDB requires us to open all pre-existing column families
     */
    @Deprecated
    public static final byte[] COLUMN_FAMILY_WILDCARDS = "CF_ABAC_***".getBytes(StandardCharsets.UTF_8);

    /**
     * List of the legacy column families that must be opened in order to successfully open a legacy format store
     */
    public static final List<byte[]> LEGACY_COLUMN_FAMILIES =
            List.of(COLUMN_FAMILY_SPO, COLUMN_FAMILY_S, COLUMN_FAMILY_P, COLUMN_FAMILY_WILDCARDS);
    /**
     * A Function to normalize RDF literal terms. Normalization means to use the node form (for literals) that
     * round-trips with TDB2 storing values. See {@link #normalize(Quad)} for more discussion.
     * <p>
     * A literal like {@code "10"^^xsd:double} has a round-trip form {@code "10e0"^^xsd:double}.
     * <p>
     * A literal like {@code "0.123456789"^^xsd:float} has a round-trip form {@code "0.12345679"^^xsd:float} due to the
     * precision of float values. Precision also affects xsd:double.
     */
    static final Function<Node, Node> normalizeFunction = NormalizeTermsTDB2::normalizeTDB2;

    /**
     * Size of the shared block cache (256MB). Index and filter blocks are routed into this cache (see
     * {@link #createBlockBasedTableConfig()}) so their memory is bounded rather than held in unbounded table-reader
     * memory.
     */
    private static final long BLOCK_CACHE_SIZE = 256L * 1024 * 1024;
    /**
     * Bound on the number of SST files RocksDB keeps open simultaneously. The default (-1) keeps every file open and
     * pins its index/filter (table reader) memory indefinitely, so this caps that native memory.
     */
    private static final int MAX_OPEN_FILES = 1024;

    private final AtomicBoolean openFlag = new AtomicBoolean(false);

    private final List<ColumnFamilyHandle> columnFamilyHandleList = new ArrayList<>();

    private RocksDB db;

    /**
     * Shared block cache for the store, backing the column family table configuration. A single instance is shared
     * across all column families (they share one {@link ColumnFamilyOptions}) so it is a single bounded ceiling for
     * cached data, index and filter blocks. Closed in {@link #closeDB()}.
     * <p>
     * May be {@code null} if the loaded RocksDB native library does not support constructing a sized
     * {@link LRUCache} (see {@link #createBlockCache()}); in that case RocksDB falls back to its own default internal
     * block cache. {@code cache_index_and_filter_blocks} still applies either way.
     * </p>
     */
    private org.rocksdb.Cache blockCache;
    /**
     * Shared bloom filter used to speed up lookups. Closed in {@link #closeDB()}.
     */
    private BloomFilter bloomFilter;

    /**
     * Convert a quad so that nulls become ANY and object literals are normalized.
     * <p>
     * Normalization is important because the label store is being used to correlate labels with quads stored in a
     * separate quad store which is typically TDB2 for production usage.  TDB2 normalizes some literals upon storage so
     * if we don't normalize them prior to storing them in RocksDB when we read data back from TDB2 we could fail to
     * find the correct label for quads containing literals due to mismatches.
     * </p>
     *
     * @return Normalized quad, or the input quad if there is no change.
     */
    public static Quad normalize(Quad quad) {
        Node g = nullToAny(quad.getGraph());
        Node s = nullToAny(quad.getSubject());
        Node p = nullToAny(quad.getPredicate());
        Node o = nullToAny(quad.getObject());
        o = normalizeFunction.apply(o);
        if (g == quad.getGraph() && s == quad.getSubject() && p == quad.getPredicate() && o == quad.getObject()) {
            return quad;
        }
        return Quad.create(g, s, p, o);
    }

    /**
     * Returns a new instance of RocksDB
     *
     * @param dbPath the path to the database
     * @return the RocksDB instance
     */
    public RocksDB openDB(final String dbPath) {
        // Create the shared block cache and bloom filter that back the column family table configuration. These must
        // exist before the column family options are built (see createBlockBasedTableConfig()).
        this.blockCache = createBlockCache();
        this.bloomFilter = createBloomFilter();

        final ColumnFamilyOptions columnFamilyOptions =
                configureRocksDBColumnFamilyOptions().setMergeOperator(new StringAppendOperator(""));

        final var defaultDescriptor = new ColumnFamilyDescriptor(RocksDB.DEFAULT_COLUMN_FAMILY, columnFamilyOptions);
        final var cfhSPODescriptor = new ColumnFamilyDescriptor(COLUMN_FAMILY_SPO, columnFamilyOptions);
        final var cfhSDescriptor = new ColumnFamilyDescriptor(COLUMN_FAMILY_S, columnFamilyOptions);
        final var cfhPDescriptor = new ColumnFamilyDescriptor(COLUMN_FAMILY_P, columnFamilyOptions);
        final var cfhWildcardsDescription = new ColumnFamilyDescriptor(COLUMN_FAMILY_WILDCARDS, columnFamilyOptions);

        if (!openFlag.compareAndSet(false, true)) {
            throw new RuntimeException("Race condition during " + CREATE_MESSAGE);
        }

        try (final DBOptions dbOptions = configureRocksDBOptions().setCreateIfMissing(true)
                                                                  .setCreateMissingColumnFamilies(true)) {
            List<ColumnFamilyDescriptor> columnFamilyDescriptorList =
                    List.of(defaultDescriptor, cfhSPODescriptor, cfhSDescriptor, cfhPDescriptor,
                            cfhWildcardsDescription);

            db = RocksDB.open(dbOptions, dbPath, columnFamilyDescriptorList, columnFamilyHandleList);
            LOG.debug("Opened RocksDB instance:{}", db);
            return db;
        } catch (RocksDBException e) {
            if (!openFlag.compareAndSet(true, false)) {
                throw new RuntimeException("Race condition during failing " + CREATE_MESSAGE);
            }
            LOG.error("Unable to open/create RocksDB label store: {}", dbPath, e);
            throw new RuntimeException("Failed " + CREATE_MESSAGE, e);
        }
    }

    public void closeDB() {
        if (openFlag.compareAndSet(true, false)) {
            try {
                LOG.debug("Closing RocksDB instance:{}", db);
                db.closeE();
            } catch (RocksDBException rocksDBException) {
                LOG.error("Problem encountered closing RocksDB instance", rocksDBException);
            } finally {
                // Close the native option objects after the database that references them. RocksDB native objects are
                // safe to close once; guard against nulls in case open failed before these were created.
                if (bloomFilter != null) {
                    bloomFilter.close();
                }
                if (blockCache != null) {
                    blockCache.close();
                }
            }
        }
    }

    /**
     * Creates the shared sized LRU block cache, if the loaded RocksDB native library supports it.
     * <p>
     * Older native libraries may not export the {@code newLRUCache} symbol that the {@link LRUCache} Java binding
     * uses, which would throw an {@link UnsatisfiedLinkError}. In that case we log a warning and return {@code null}
     * so the caller falls back to RocksDB's default internal block cache rather than failing to open the store.
     * </p>
     *
     * @return a sized {@link LRUCache}, or {@code null} if one cannot be constructed
     */
    protected org.rocksdb.Cache createBlockCache() {
        try {
            return new LRUCache(BLOCK_CACHE_SIZE);
        } catch (Throwable t) {
            // Typically UnsatisfiedLinkError when the native library predates the LRUCache binding's expected symbol
            LOG.warn("Unable to create a {} byte LRU block cache ({}); falling back to RocksDB's default block cache",
                     BLOCK_CACHE_SIZE, t.toString());
            return null;
        }
    }

    /**
     * Creates the shared bloom filter used as the block based table filter policy, if the loaded RocksDB native
     * library supports it.
     * <p>
     * Older native libraries may not export the {@code createNewBloomFilter} symbol that the {@link BloomFilter} Java
     * binding uses, which would throw an {@link UnsatisfiedLinkError}. In that case we log a warning and return
     * {@code null} so the caller opens the store without a bloom filter rather than failing to open it. This mirrors
     * the graceful degradation in {@link #createBlockCache()}.
     * </p>
     *
     * @return a {@link BloomFilter}, or {@code null} if one cannot be constructed
     */
    protected BloomFilter createBloomFilter() {
        try {
            return new BloomFilter(10.0);
        } catch (Throwable t) {
            // Typically UnsatisfiedLinkError when the native library predates the BloomFilter binding's expected symbol
            LOG.warn("Unable to create a BloomFilter ({}); opening store without a bloom filter", t.toString());
            return null;
        }
    }

    /**
     * Set up performance tuning options for column family options as recommended by <a href=
     * "https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning">Setup-Options-and-Basic-Tuning</a>
     *
     * @return column family options configured as recommended
     */
    protected ColumnFamilyOptions configureRocksDBColumnFamilyOptions() {
        var options = new ColumnFamilyOptions();
        options.setLevelCompactionDynamicLevelBytes(true);
        options.setCompressionType(CompressionType.LZ4_COMPRESSION);
        options.setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION);
        // Table configuration MUST be applied here (column-family level) and not on the database Options - the latter
        // is only used to derive DBOptions when opening, which does not carry the table format configuration.
        options.setTableFormatConfig(createBlockBasedTableConfig());

        return options;
    }

    /**
     * Builds the block based table configuration applied to the column families. This is where the shared
     * {@link #blockCache} and {@link #bloomFilter} are actually wired in, and where {@code cache_index_and_filter_blocks}
     * is enabled so index and filter blocks are accounted for within (and bounded by) the shared block cache rather
     * than being held in unbounded table-reader memory.
     * <p>
     * <strong>NB:</strong> {@link #blockCache} and {@link #bloomFilter} must be initialised before this is called;
     * {@link #openDB(String)} guarantees this ordering.
     * </p>
     *
     * @return block based table configuration
     */
    protected BlockBasedTableConfig createBlockBasedTableConfig() {
        var tableOptions = new BlockBasedTableConfig();
        // Only set an explicit (sized) block cache when one was successfully created; otherwise let RocksDB use its
        // own default internal cache. cache_index_and_filter_blocks (below) routes index/filter blocks into whichever
        // cache is in effect.
        if (this.blockCache != null) {
            tableOptions.setBlockCache(this.blockCache);
        }
        LOG.debug("blockSize {} to {}", tableOptions.blockSize(), 16 * 1024);
        tableOptions.setBlockSize(16 * 1024);
        LOG.debug("cacheIndexAndFilterBlocks {} to {}", tableOptions.cacheIndexAndFilterBlocks(), true);
        tableOptions.setCacheIndexAndFilterBlocks(true);
        // Treat index/filter blocks as high priority so they are evicted only after regular data blocks
        tableOptions.setCacheIndexAndFilterBlocksWithHighPriority(true);
        LOG.debug("pinL0FilterAndIndexBlocksInCache {} to {}", tableOptions.pinL0FilterAndIndexBlocksInCache(), true);
        tableOptions.setPinL0FilterAndIndexBlocksInCache(true);
        // Only set an explicit filter policy when a bloom filter was successfully created; otherwise let RocksDB open
        // the store without one (e.g. when the loaded native library predates the BloomFilter binding's symbol).
        if (this.bloomFilter != null) {
            LOG.debug("filterPolicy {} to {}", tableOptions.filterPolicy(), this.bloomFilter);
            tableOptions.setFilterPolicy(this.bloomFilter);
        }
        LOG.debug("formatVersion {} to {}", tableOptions.formatVersion(), 5);
        tableOptions.setFormatVersion(5);
        return tableOptions;
    }

    /**
     * Set up performance tuning options for database options as recommended by <a href=
     * "https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning">Setup-Options-and-Basic-Tuning</a>
     *
     * @return database options configured as recommended
     */
    protected DBOptions configureRocksDBOptions() {
        Options options = new Options();
        configureRocksOptions(options);
        return new DBOptions(options);
    }

    /**
     * Set up performance tuning options for database options as recommended by <a href=
     * "https://github.com/facebook/rocksdb/wiki/Setup-Options-and-Basic-Tuning">Setup-Options-and-Basic-Tuning</a>
     */
    public static void configureRocksOptions(Options options) {
        LOG.debug("Configure RocksDB options from defaults to recommended:");
        LOG.debug("maxBackgroundJobs {} to {}", options.maxBackgroundJobs(), 6);
        options.setMaxBackgroundJobs(6);
        LOG.debug("bytesPerSync {} to {}", options.bytesPerSync(), 1048576);
        options.setBytesPerSync(1048576);
        LOG.debug("compactionPriority {} to {}", options.compactionPriority(), CompactionPriority.MinOverlappingRatio);
        options.setCompactionPriority(CompactionPriority.MinOverlappingRatio);
        LOG.debug("maxOpenFiles {} to {}", options.maxOpenFiles(), MAX_OPEN_FILES);
        options.setMaxOpenFiles(MAX_OPEN_FILES);

        // NB: The block based table configuration (block cache, bloom filter, cache_index_and_filter_blocks etc.) is a
        //     column-family level concern and is applied via configureRocksDBColumnFamilyOptions() /
        //     createBlockBasedTableConfig(). It must NOT be set here: this Options instance is only ever used to derive
        //     DBOptions (new DBOptions(options)) when opening the database, and DBOptions does not carry the table
        //     format configuration, so anything set here would be silently discarded.
    }

    /**
     * Returns a transactional instance based on the underlying RocksDB instance
     *
     * @return - the transactional RocksDB
     */
    public TransactionalRocksDB getTransactionalRocksDB(Cache<Quad, Label> cache) {
        return new TransactionalRocksDB(db, cache);
    }

    /**
     * Accessor to the ColumnFamilyHandle used within the RocksDB instance
     *
     * @param index the column index to remove and return
     * @return the removed ColumnFamilyHandle
     */
    public ColumnFamilyHandle removeFromColumnFamilyHandleList(int index) {
        return columnFamilyHandleList.remove(index);
    }

}
