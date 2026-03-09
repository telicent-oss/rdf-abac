/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac.labels.store.rocksdb.legacy;

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.labels.*;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.atlas.lib.CacheFactory;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.tdb2.sys.NormalizeTermsTDB2;
import org.rocksdb.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;

import static io.telicent.jena.abac.core.VocabAuthzDataset.pLabelsStoreByteBufferSize;
import static io.telicent.jena.abac.labels.Labels.LOG;
import static org.apache.jena.sparql.util.NodeUtils.nullToAny;

/**
 * A labels store implemented using the RocksDB (key,value)-store.
 * <p>
 * An instance may store label nodes by string value, or by id. The difference is encoded by the {@link StoreFmt}
 * supplied at the constructor, which is responsible for taking singleton nodes and node-triples and formatting them as
 * a byte-sequence to supply to RocksDB as the key part of the (key,value)-pair.
 * <p>
 * If label nodes are stored by id, the {@link StoreFmt} will contain a
 * {@link org.apache.jena.tdb2.store.nodetable.NodeTable} with which to perform the mapping, though that is invisible to
 * the {@code LabelsStoreRocksDB}.
 *
 * @deprecated This is the legacy RocksDB label store maintained for backwards compatibility and to allow forward
 * migration of data to the newer RocksDB label store
 */
@Deprecated
public class LegacyLabelsStoreRocksDB implements LabelsStore {

    final static AtomicLong keyTotalSize = new AtomicLong();
    final static AtomicLong valueTotalSize = new AtomicLong();

    public final static int DEFAULT_BUFFER_CAPACITY = 1048576;

    /**
     * Cache of triple lookup in {@link #labelForSPO}
     * <p>
     * The cache is maintained by {@link #add(Quad, Label)}
     * </p>
     */
    private static final int LABEL_LOOKUP_CACHE_SIZE = 1_000_000;
    // Hit cache of triple to list of strings (labels).
    private final Cache<Quad, Label> labelCache = CacheFactory.createCache(LABEL_LOOKUP_CACHE_SIZE);

    /**
     * We maintain a buffer per-thread for key encoding and label encoding to avoid continual re-allocation.
     */
    protected final ThreadLocal<ByteBuffer> keyBuffer;
    protected final ThreadLocal<ByteBuffer> labelsBuffer;

    protected final StoreFmt.Encoder encoder;
    protected final StoreFmt.Parser parser;

    protected final ThreadLocal<ReadOptions> readOptions = ThreadLocal.withInitial(ReadOptions::new);

    protected final ThreadLocal<ByteBuffer> valueBuffer;

    private final int bufferCapacity;

    private final String dbPath;

    /**
     * A Function to normalize RDF literal terms. Normalization means to use the node form (for literals) that
     * round-trips with TDb2 storing values. Normalization is applied on storage
     * ({@link #addRule(Node, Node, Node, Label)}) and lookup ({@link #labelForQuad(Quad)}).
     * <p>
     * A literal like "10"^^xsd:double has a round-trip form "10e0"^^xsd:double.
     * <p>
     * A literal like "0.123456789"^^xsd:float has a round-tripform 0.12345679"^^xsd:float due to the precision of float
     * values. Precision also affects xsd:double.
     */
    private static final Function<Node, Node> normalizeFunction = NormalizeTermsTDB2::normalizeTDB2;

    private final RocksDBHelper helper;
    protected TransactionalRocksDB txRocksDB;
    private RocksDB rocksDB;

    /**
     * A RocksDB column family for each of the layers of the label lookup algorithm.
     */
    protected ColumnFamilyHandle cfhSPO;

    public StoreFmt.Encoder getEncoder() {
        return this.encoder;
    }

    public StoreFmt.Parser getParser() {
        return this.parser;
    }

    private List<ColumnFamilyHandle> allColumnFamilies() {
        return List.of(cfhSPO);
    }

    private ByteBuffer allocateKVBuffer() {
        return ByteBuffer.allocateDirect(bufferCapacity).order(ByteOrder.LITTLE_ENDIAN);
    }

    // Cached parsing on attribute expressions.
    // Use a small cache to cover the common case of all the labels being the same.
    private final static Cache<String, AttributeExpr> cache = CacheFactory.createOneSlotCache();

    /**
     * Parse an attribute expressions - a label
     */
    private static AttributeExpr parseAttrExpr(String str) {
        return cache.get(str, AE::parseExpr);
    }

    /**
     * Obtain the byte buffer capacity value from configuration if available.
     *
     * @param resource RDF Node representing the configuration
     */
    private static int getByteBufferSize(Resource resource) {
        if (resource != null && resource.hasProperty(pLabelsStoreByteBufferSize)) {
            Statement statement = resource.getProperty(pLabelsStoreByteBufferSize);
            try {
                int capacity = statement.getInt();
                if (capacity > 0) {
                    return capacity;
                } else {
                    throw new RuntimeException("The RocksDB buffer capacity is invalid value.");
                }
            } catch (IllegalArgumentException e) {
                throw new RuntimeException("The RocksDB buffer capacity is in wrong format.");
            }
        }
        return DEFAULT_BUFFER_CAPACITY;
    }

    /**
     * Create a RocksDB-based label store
     *
     * @param dbRoot   file into which to save the database
     * @param storeFmt formatter to transform node(s) into byte arrays.
     */
    public LegacyLabelsStoreRocksDB(final RocksDBHelper helper, final File dbRoot, final StoreFmt storeFmt,
                                    Resource resource) {
        this.bufferCapacity = getByteBufferSize(resource);
        this.keyBuffer = ThreadLocal.withInitial(this::allocateKVBuffer);
        this.valueBuffer = ThreadLocal.withInitial(this::allocateKVBuffer);
        this.labelsBuffer = ThreadLocal.withInitial(this::allocateKVBuffer);

        this.encoder = storeFmt.createEncoder();
        this.parser = storeFmt.createParser();

        this.dbPath = dbRoot.getAbsolutePath();
        this.helper = helper;
        openDB();
    }

    @SuppressWarnings("resource")
    private void openDB() {
        rocksDB = helper.openDB(dbPath);
        txRocksDB = helper.getTransactionalRocksDB();
        // Ignore the default CFH which we never use.
        helper.removeFromColumnFamilyHandleList(0);

        cfhSPO = helper.removeFromColumnFamilyHandleList(0);
    }

    @Override
    public Label labelForQuad(Quad quad) {
        Quad normalized = normalize(quad);
        return labelCache.get(quad, t -> labelForQuad(normalized.getGraph(), normalized.getSubject(),
                                                      normalized.getPredicate(), normalized.getObject()));
    }

    // Convert a triple so that nulls become ANY and object literals are normalized.
    // Returns the input object if there is no change.
    private static Quad normalize(Quad quad) {
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
     * Perform a lookup in the labels store, given the SPO-triple (encoded as 3 separate nodes), return the (list of)
     * labels which are the answer to "what is the most specific set of labels for this triple ?" - first, if a value
     * (list of labels) is held for SPO is held, return that - second, if a value (list of labels) is held for SP_ (a
     * wildcard on object) is held, return that - third, if a value (list of labels) is held for S__ (a wildcard for
     * predicate and object) - fourth, _P_ (a wildcard for subject and object) - fifth, a complete wildcard/backstop
     * list of values.
     *
     * @param subject   part of the triple
     * @param predicate part of the triple
     * @param object    part of the triple
     * @return a list/set of labels
     */
    private Label labelForQuad(final Node graph, final Node subject, final Node predicate, final Node object) {
        if (!graph.isConcrete() || !subject.isConcrete() || !predicate.isConcrete() || !object.isConcrete()) {
            throw new LabelsException(
                    "Asked for labels for a quad with wildcards: " + NodeFmtLib.strNodesTTL(graph, subject, predicate,
                                                                                            object));
        } else if (!Objects.equals(graph, Quad.defaultGraphIRI)) {
            throw new LabelsException(
                    "Asked for label for a quad outside the default graph (" + NodeFmtLib.strTTL(
                            graph) + "), legacy RocksDB store only supported labelling triples in the default graph");
        }

        try {
            return labelForSPO(subject, predicate, object);
        } catch (RocksDBException e) {
            throw new RuntimeException(
                    "Label store failed on lookup " + NodeFmtLib.displayStr(Triple.create(subject, predicate, object)),
                    e);
        }
    }

    @Override
    public void forEach(BiConsumer<Quad, Label> action) {
        throw new NotImplemented(this.getClass().getSimpleName() + ".forEach");
    }

    /**
     * Clear the triple lookup cache
     */
    public void clearTripleLookupCache() {
        labelCache.clear();
    }

    /**
     * Get the labels held for a particular key.
     * <p>
     * There may be multiple entries of the form (count,lengths[],strings[]) as multiple add()s will result in RocksDB
     * merging the values it has received, by concatenation.
     *
     * @param valueBuffer holding the labels
     * @return the list of labels, which contains a set of labels
     */
    private List<Label> getLabels(final ByteBuffer valueBuffer) {
        Collection<Label> set = new HashSet<Label>();
        while (valueBuffer.position() < valueBuffer.limit()) {
            parser.parseLabels(valueBuffer, set);
        }
        return new ArrayList<>(set);
    }

    /**
     * Perform the core of lookup by accessing each of the RocksDB column families in priority order
     * <p>
     * Is there a match for S,P,O or S,P,_ ? The SPO column family also stores values for S,P,Any. These are fetched as
     * a single {@code multiGet()}, but it may be more efficient, depending on workload, to do 2 single gets, if the
     * first (S,P,O) usually succeeds this will avoid the overhead of a {@code multiGet()} The choice depends on what is
     * the common case for whether a particular S,P,O key will have its own label value stored, or whether S,P,Any is
     * more likely to be the first hit on lookup.
     * <p>
     * Using {@code multiGet()} avoids the need to perform scans of iterators, which in turn would require a custom
     * comparator so that Rocks stores keys in natural S,P,O < S,P,* order.. If changing the implementation to do this
     * is contemplated, any comparator MUST be written as a C++ comparator (not Java) in order not to destroy
     * performance.
     * </p>
     *
     * @param subject   part of the triple
     * @param predicate part of the triple
     * @param object    part of the triple
     * @return the label, or {@code null} if no such label
     * @throws RocksDBException if something went wrong with the database lookup
     */
    private Label labelForSPO(final Node subject, final Node predicate, final Node object) throws
            RocksDBException {
        if (rocksDB == null) {
            throw new RuntimeException("The RocksDB labels store appears to be closed.");
        }

        ByteBuffer key = keyBuffer.get().clear();
        ByteBuffer valueBuffer = this.valueBuffer.get().clear();

        encoder.formatTriple(key, subject, predicate, object);
        ReadOptions readOptionsInstance = readOptions.get();

        // Checking S,P,O
        if (rocksDB.get(cfhSPO, readOptionsInstance, key.flip(), valueBuffer) != RocksDB.NOT_FOUND) {
            List<Label> labels = getLabels(valueBuffer);
            if (labels.isEmpty()) {
                return null;
            } else if (labels.size() > 1) {
                throw new LabelsException("Multiple labels against a single triple is no longer permitted");
            } else {
                return labels.getFirst();
            }
        }
        return null;
    }

    @Override
    public Transactional getTransactional() {
        return this.txRocksDB;
    }

    private void write(TransactionalRocksDB transactionalRocksDB, ColumnFamilyHandle columnFamilyHandle, ByteBuffer key,
                       ByteBuffer value) {
        if (LOG.isDebugEnabled()) {
            keyTotalSize.addAndGet(key.limit());
            valueTotalSize.addAndGet(value.limit());
        }
        transactionalRocksDB.put(columnFamilyHandle, key, value);
    }

    /**
     * Add (or replace) an entry to the labels store keyed by a triple S,P,O
     *
     * @param quad  Quad to label
     * @param label Label to associate with the supplied triple
     */
    @Override
    public void add(Quad quad, Label label) {
        Quad normalized = normalize(quad);
        Label cachedLabel = labelCache.getIfPresent(normalized);

        if (Objects.equals(cachedLabel, label)) {
            // Labels are the same, no need to update
            return;
        }

        // Remove the old entry if it exists
        if (cachedLabel != null) {
            labelCache.remove(normalized);
        }

        if (!Objects.equals(Quad.defaultGraphIRI, quad.getGraph())) {
            throw new LabelsException(
                    "Legacy RocksDB store only supports storing labels for quads in the default graph");
        }

        // Add the new rule and update the cache, if a previous entry or cache is under-populated
        addRule(normalized.getSubject(), normalized.getPredicate(), normalized.getObject(), label);
        if (cachedLabel != null || (labelCache.size() < LABEL_LOOKUP_CACHE_SIZE)) {
            labelCache.put(normalized, label);
        }
    }

    /**
     * Perform the code of adding a rule by deciding which pattern and column family to write to. This is the single
     * place that all updates happen.
     *
     * @param subject  Subject of the triple
     * @param property Predicate of the triple
     * @param object   Object of the triple, will be normalized if a literal
     * @param label    Label to associate with the supplied triple
     */
    private void addRule(final Node subject, final Node property, Node object, final Label label) {
        object = normalizeFunction.apply(object);
        addRuleWorker(subject, property, object, label);
    }

    private void addRuleWorker(final Node subject, final Node property, final Node object, final Label label) {

        // Single point for all adding to the labels store.
        if (rocksDB == null) {
            throw new RuntimeException("The RocksDB labels store appears to be closed.");
        }
        LOG.debug("addRule ({},{},{}) -> {}", subject, property, object, label);

        addRuleSPO(subject, property, object, label);
        count++;
    }

    /**
     * Add a rule for a specific SPO to the SPO column family
     *
     * @param subject   Subject of the triple
     * @param predicate Predicate of the triple
     * @param object    Object of the triple
     * @param label     Label to associate with the supplied triple
     */
    private void addRuleSPO(final Node subject, final Node predicate, final Node object, final Label label) {
        var key = keyBuffer.get().clear();
        encoder.formatTriple(key, subject, predicate, object);
        var labels = labelsBuffer.get().clear();
        encoder.formatLabels(labels, List.of(label));
        write(txRocksDB, cfhSPO, key.flip(), labels.flip());
    }

    /**
     * Remove any labels for a specific triple. This does not affect any patterns.
     */
    @Override
    public void remove(Quad quad) {
        labelCache.remove(quad);
        throw new UnsupportedOperationException("LabelsStore.remove not supported by LabelsStoreRockDB");
    }

    // Information gathering count of how many labels have been added during the current session
    // This does not account for duplicates of the same label (they could as two), nor does it account for labels
    // already defined in the store on disk
    // This indicative count is primarily for development.
    private static long count = 0;

    @Override
    public Graph asGraph() {
        return Graph.emptyGraph;
    }

    /**
     * The properties returned by the RocksDB labels store include a number of metrics. {@code size} is accurate but
     * expensive to calculate; {@code count} is maintained by counting insertions using this instance of the store and
     * is an approximate metric which RocksDB can calculate efficiently.
     *
     * @return the properties of the RocksDB labels store
     */
    @Override
    public Map<String, String> getProperties() {
        final var properties = new HashMap<String, String>();

        properties.put("approxsize", Long.toString(approximateSizes()));
        properties.put("size", Long.toString(expensiveCount()));
        properties.put("count", Long.toString(count));

        if (LOG.isDebugEnabled()) {
            properties.put("keyTotalSize", "" + keyTotalSize.get());
            properties.put("valueTotalSize", "" + valueTotalSize.get());
        }

        return properties;
    }

    private long approximateSizes() {
        var first = new byte[0];
        var last = new byte[16];
        Arrays.fill(last, (byte) 0xff);

        return getApproximateSize(cfhSPO, first, last);
    }

    /**
     * @param cfh   column family to get size of
     * @param first must be a logically valid key in the keys of cfh (doesn't have to be in the store)
     * @param last  must be a logically valid key in the keys of cfh (doesn't have to be in the store)
     * @return a number representing an approximate size (the best effort)
     */
    private long getApproximateSize(ColumnFamilyHandle cfh, byte[] first, byte[] last) {
        final long[] sizes = rocksDB.getApproximateSizes(cfh, List.of(new Range(new Slice(first), new Slice(last))),
                                                         SizeApproximationFlag.INCLUDE_FILES,
                                                         SizeApproximationFlag.INCLUDE_MEMTABLES);

        if (sizes.length != 1) {
            throw new RuntimeException("Unexpected size range of RocksDB column family: " + sizes.length);
        }
        return sizes[0];
    }

    private long expensiveCount() {
        return getExpensiveCount(cfhSPO);
    }

    private int getExpensiveCount(ColumnFamilyHandle cfh) {
        try (var it = rocksDB.newIterator(cfh)) {
            var count = 0;
            for (it.seekToFirst(); it.isValid(); it.next()) {
                count++;
            }
            return count;
        }
    }

    private boolean columnFamilyIsEmpty(ColumnFamilyHandle cfh) {
        try (var it = rocksDB.newIterator(cfh)) {
            it.seekToFirst();
            return (!it.isValid());
        }
    }

    @Override
    public boolean isEmpty() {
        return columnFamilyIsEmpty(cfhSPO);
    }

    /**
     * Invoke compaction on the underlying RocksDB
     * <p>
     * After compaction, the RocksDB database should have a more predictable and consistent performance and storage
     * footprint for the workload it has been applied to.
     */
    public void compact() {
        LOG.info("RocksDB label store: perform compaction");
        try {
            for (var cfh : allColumnFamilies()) {
                var from = new byte[] {};
                var to = new byte[16];
                for (int i = 0; i < 16; i++)
                    to[i] = (byte) -1;
                rocksDB.compactRange(cfh, from, to);
            }
        } catch (RocksDBException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        helper.closeDB();
        rocksDB = null;
        // RocksDB knows which cfh(s) it owns, and closes them as part of db.close(),
        // so we don't have to close them.
        // But just in case the now-closed CFs contain dangling references to
        // de-allocated C++ structures,
        // we forget the references.
        cfhSPO = null;
    }

    /**
     * Back up the underlying Rocks DB instance to given path
     *
     * @param path location of backup
     */
    public void backup(String path) {
        // Create a backup engine
        try (BackupEngine backupEngine = BackupEngine.open(rocksDB.getEnv(), new BackupEngineOptions(path))) {
            LOG.info("Backing Up Labels Store (begin): {}", path);
            backupEngine.createNewBackup(rocksDB, true);
            LOG.info("Backing Up Labels Store (finished): {}", path);
        } catch (Exception exception) {
            LOG.error("Backing Up Labels Store (failed)", exception);
            throw new LabelsException("Backup Labels Store failed", exception);
        }
    }

    /**
     * Replace existing Rocks DB instance with given path.
     *
     * @param path location of backup
     */
    public void restore(String path) {
        // Create a backup engine
        try (BackupEngine backupEngine = BackupEngine.open(rocksDB.getEnv(), new BackupEngineOptions(path))) {
            LOG.info("Restoring Labels Store (begin): {}", path);
            LOG.info("Restoring Labels Store (closing DB): {}", rocksDB.isClosed());
            close();
            LOG.info("Restoring Labels Store (from backup)");
            backupEngine.restoreDbFromLatestBackup(this.dbPath, path, new RestoreOptions(false));
            LOG.info("Restoring Labels Store (re-opening DB)");
            openDB();
            LOG.info("Restoring Labels Store (clearing cache): {}", labelCache.size());
            labelCache.clear();
            LOG.info("Restoring Labels Store (cache cleared): {}", labelCache.size());
            LOG.info("Restoring Labels Store (finished): {}", path);
        } catch (Exception exception) {
            LOG.error("Restoring Labels Store (failed)", exception);
            throw new LabelsException("Restore Labels Store failed", exception);
        }
    }
}
