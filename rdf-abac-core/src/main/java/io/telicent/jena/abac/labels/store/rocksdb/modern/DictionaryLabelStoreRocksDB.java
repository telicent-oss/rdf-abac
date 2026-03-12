package io.telicent.jena.abac.labels.store.rocksdb.modern;

import io.telicent.jena.abac.labels.*;
import io.telicent.smart.cache.storage.labels.rocksdb.RocksDbLabelsStore;
import io.telicent.smart.cache.storage.rocksdb.TransactionContext;
import org.apache.jena.graph.Graph;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.riot.out.NodeFmtLib;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.rocksdb.RocksDBException;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

/**
 * A labels store backed by RocksDB where the labels are dictionary encoded to reduce storage consumption
 * <p>
 * This basically wraps the {@link RocksDbLabelsStore} implementation into the RDF-ABAC {@link LabelsStore} API.
 * </p>
 */
public class DictionaryLabelStoreRocksDB extends RocksDbLabelsStore implements LabelsStore {

    private static final int MAX_BYTES_PER_HASH = 16;

    private final StoreFmt storeFmt;
    private final StoreFmt.Encoder encoder;
    private final StoreFmt.Parser parser;

    /**
     * Creates a new dictionary encoded labels store backed by RocksDB
     *
     * @param dbPath   Database directory
     * @param storeFmt Store Format
     * @throws IllegalArgumentException Thrown if an unsupported store format is provided
     * @throws IOException              Thrown if there's a problem accessing the database directory
     * @throws RocksDBException         Thrown if there's a problem accessing the RocksDB database in the given
     *                                  directory
     */
    public DictionaryLabelStoreRocksDB(File dbPath, StoreFmt storeFmt) throws IOException, RocksDBException {
        super(dbPath);

        this.storeFmt = Objects.requireNonNull(storeFmt);
        if (!(this.storeFmt instanceof StoreFmtByHash)) {
            throw new IllegalArgumentException("Only StoreFmtByHash is currently supported");
        }
        this.encoder = this.storeFmt.createEncoder();
        this.parser = this.storeFmt.createParser();
    }

    protected final ThreadLocal<ByteBuffer> keyBuffer =
            ThreadLocal.withInitial(() -> ByteBuffer.allocateDirect(4 * MAX_BYTES_PER_HASH).order(
                    ByteOrder.LITTLE_ENDIAN));

    @Override
    public Label labelForQuad(Quad quad) {
        if (!quad.isConcrete()) {
            throw new LabelsException(
                    "Asked for labels for a quad with wildcards: " + NodeFmtLib.strNodesTTL(quad.getGraph(),
                                                                                            quad.getSubject(),
                                                                                            quad.getPredicate(),
                                                                                            quad.getObject()));
        }
        ByteBuffer buffer = keyBuffer.get().clear();
        this.encoder.formatQuad(buffer, quad.getGraph(), quad.getSubject(), quad.getPredicate(), quad.getObject());
        buffer.flip();

        byte[] key = asByteArray(buffer);
        return new Label(this.getLabelAsBytes(key), StandardCharsets.UTF_8);
    }

    static byte[] asByteArray(ByteBuffer buffer) {
        byte[] key = new byte[buffer.limit()];
        buffer.get(key);
        return key;
    }

    @Override
    public Transactional getTransactional() {
        return new JenaTransactionWrapper(this);
    }

    @Override
    public void add(Quad quad, Label label) {
        if (!quad.isConcrete()) {
            throw new LabelsException(
                    "Tried to set labels for a quad with wildcards: " + NodeFmtLib.strNodesTTL(quad.getGraph(),
                                                                                               quad.getSubject(),
                                                                                               quad.getPredicate(),
                                                                                               quad.getObject()));
        }
        ByteBuffer buffer = keyBuffer.get().clear();
        this.encoder.formatQuad(buffer, quad.getGraph(), quad.getSubject(), quad.getPredicate(), quad.getObject());
        buffer.flip();
        byte[] key = asByteArray(buffer);

        // Store the label and associated the label with this quad as a single atomic transaction
        // Calling beginNested() ensures that when the called methods call begin() they share the same transaction
        // rather than performing their actions in independent transactions
        try (TransactionContext context = this.beginNested()) {
            long labelId = this.idForLabel(label.getData());
            this.setLabel(key, labelId);
        }
    }

    @Override
    public void remove(Quad quad) {
        throw new UnsupportedOperationException("Removing labels is not supported");
    }

    @Override
    public boolean isEmpty() {
        try (TransactionContext context = this.begin()) {
            return context.isEmpty(this.getHandle(KEYS_TO_LABELS_CF));
        }
    }

    @Override
    public void forEach(BiConsumer<Quad, Label> action) {
        throw new UnsupportedOperationException("Original quads are not stored so cannot be iterated over");
    }

    @Override
    public Graph asGraph() {
        return null;
    }

    @Override
    public Map<String, String> getProperties() {
        return Map.of();
    }

    private static final class JenaTransactionWrapper implements Transactional {

        private final DictionaryLabelStoreRocksDB store;
        private TransactionContext context = null;

        JenaTransactionWrapper(DictionaryLabelStoreRocksDB store) {
            this.store = store;
        }

        @Override
        public void begin(TxnType type) {
            this.context = this.store.beginNested();
        }

        @Override
        public boolean promote(Promote mode) {
            return true;
        }

        @Override
        public void commit() {
            try {
                this.context.commit();
            } catch (RocksDBException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void abort() {
            this.context.close();
        }

        @Override
        public void end() {
            this.context.close();
        }

        @Override
        public ReadWrite transactionMode() {
            return ReadWrite.WRITE;
        }

        @Override
        public TxnType transactionType() {
            return TxnType.WRITE;
        }

        @Override
        public boolean isInTransaction() {
            return this.context != null && this.context.isActive();
        }
    }
}
