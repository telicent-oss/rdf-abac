package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsException;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import io.telicent.jena.abac.AbstractionTransactionalTests;
import org.apache.jena.graph.Node;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.sse.SSE;
import org.apache.jena.system.Txn;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rocksdb.RocksDBException;
import io.telicent.jena.abac.labels.StoreFmt;

import java.nio.file.Files;
import java.util.List;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("java:S5786")
public class TestTransactionalModern extends AbstractionTransactionalTests {

    @Override
    protected LabelsStore create() {
        try {
            return new DictionaryLabelStoreRocksDB(Files.createTempDirectory("rocks").toFile(),
                                                   new StoreFmtByHash(HasherUtil.createXX128Hasher()));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected boolean enforcesReadOnlyTransactions() {
        // The modern store distinguishes read-only transactions and rejects writes made within them
        return true;
    }

    @Test
    void givenAbortedWrite_whenRetryingSameLabel_thenRetryIsCommitted() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();

            transactional.begin(TxnType.WRITE);
            store.add(SSE.parseTriple("(:s :p :o)"), LABEL);
            transactional.abort();

            transactional.begin(TxnType.WRITE);
            store.add(SSE.parseTriple("(:s :p :o)"), LABEL);
            transactional.commit();

            Assertions.assertEquals(LABEL, store.labelForTriple(SSE.parseTriple("(:s :p :o)")));
        }
    }

    @Test
    void givenDuplicateQuads_whenAddingBatch_thenWritesEachQuadOnce() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            var first = SSE.parseQuad("(:g :s1 :p :o)");
            var second = SSE.parseQuad("(:g :s2 :p :o)");

            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(first, first, second), LABEL);
            transactional.commit();

            Assertions.assertEquals(LABEL, store.labelForQuad(first));
            Assertions.assertEquals(LABEL, store.labelForQuad(second));
            Assertions.assertEquals("3", store.getProperties().get("labelAddAttempts"));
            Assertions.assertEquals("1", store.getProperties().get("labelCacheNoOps"));
            Assertions.assertEquals("2", store.getProperties().get("labelWrites"));
            Assertions.assertEquals(3L, store.getMetrics().get(LabelsStore.METRIC_LABEL_ADD_ATTEMPTS));
            Assertions.assertEquals(1L, store.getMetrics().get(LabelsStore.METRIC_LABEL_CACHE_NO_OPS));
            Assertions.assertEquals(2L, store.getMetrics().get(LabelsStore.METRIC_LABEL_WRITES));
        }
    }

    @Test
    void givenExistingQuad_whenBatchChangesLabel_thenNewLabelWins() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            var quad = SSE.parseQuad("(:g :s :p :o)");
            Label replacement = Label.fromText("replacement");

            transactional.begin(TxnType.WRITE);
            store.add(quad, LABEL);
            transactional.commit();

            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(quad, quad), replacement);
            transactional.commit();

            Assertions.assertEquals(replacement, store.labelForQuad(quad));
            Assertions.assertEquals(2L, store.getMetrics().get(LabelsStore.METRIC_LABEL_WRITES));
        }
    }

    @Test
    public void givenCachedQuadWithSameLabel_whenAddingBatch_thenNothingIsWritten() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            var quad = SSE.parseQuad("(:g :s :p :o)");

            transactional.begin(TxnType.WRITE);
            store.add(quad, LABEL);
            transactional.commit();

            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(quad), LABEL);
            transactional.commit();

            Assertions.assertEquals(LABEL, store.labelForQuad(quad));
            Assertions.assertEquals(2L, store.getMetrics().get(LabelsStore.METRIC_LABEL_ADD_ATTEMPTS));
            Assertions.assertEquals(1L, store.getMetrics().get(LabelsStore.METRIC_LABEL_CACHE_NO_OPS));
            Assertions.assertEquals(1L, store.getMetrics().get(LabelsStore.METRIC_LABEL_WRITES));
        }
    }

    @Test
    void givenWildcardQuadInBatch_whenAddingBatch_thenRejectedAndNothingIsWritten() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            Quad concrete = SSE.parseQuad("(:g :s :p :o)");
            Quad wildcard = Quad.create(concrete.getGraph(), Node.ANY, concrete.getPredicate(), concrete.getObject());

            transactional.begin(TxnType.WRITE);
            List<Quad> quadList = List.of(concrete, wildcard);
            Assertions.assertThrows(LabelsException.class, () -> store.addAll(quadList, LABEL));
            transactional.abort();

            // Validation happens before anything is written, so the concrete quad earlier in the batch is not stored
            Assertions.assertNotEquals(LABEL, store.labelForQuad(concrete));
            Assertions.assertEquals(0L, store.getMetrics().get(LabelsStore.METRIC_LABEL_WRITES));
        }
    }

    @Test
    void givenNullArguments_whenAddingBatch_thenRejected() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            Quad quad = SSE.parseQuad("(:g :s :p :o)");
            List quadList = List.of(quad);
            transactional.begin(TxnType.WRITE);
            Assertions.assertThrows(NullPointerException.class, () -> store.addAll(null, LABEL));
            Assertions.assertThrows(NullPointerException.class, () -> store.addAll(quadList, null));
            Assertions.assertThrows(NullPointerException.class, () -> store.add(quad, null));
            transactional.abort();
        }
    }

    @Test
    void givenUncachedQuads_whenBulkWriteCommits_thenCacheIsNotPopulatedButLabelsAreStored() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            Quad quad = SSE.parseQuad("(:g :s :p :o)");

            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(quad), LABEL);
            transactional.commit();

            // A repeat isn't skipped, because bulk writes inside a transaction no longer populate the cache
            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(quad), LABEL);
            transactional.commit();

            Assertions.assertEquals(LABEL, store.labelForQuad(quad));
            Assertions.assertEquals(0L, store.getMetrics().get(LabelsStore.METRIC_LABEL_CACHE_NO_OPS));
            Assertions.assertEquals(2L, store.getMetrics().get(LabelsStore.METRIC_LABEL_WRITES));
        }
    }

    @Test
    void givenReaderEndsThenAnotherReaderCachesOldLabelDuringBulkRelabel_whenRelabelCommits_thenNewLabelIsReturned()
            throws Exception {
        ExecutorService reader = Executors.newSingleThreadExecutor();
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            Quad quad = SSE.parseQuad("(:g :s :p :o)");
            Label replacement = Label.fromText("replacement");

            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(quad), LABEL);
            transactional.commit();

            // Writer relabels the (uncached) quad but hasn't committed yet
            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(quad), replacement);

            // Meanwhile a reader transaction on another thread sees the committed label and ends.  Ending this reader
            // must not consume the writer transaction's pending cache invalidation.
            Label seenByReader = reader.submit(() -> Txn.calculateRead(transactional, () -> store.labelForQuad(quad)))
                                       .get(30, TimeUnit.SECONDS);
            Assertions.assertEquals(LABEL, seenByReader);

            // After that reader ends it has cleared the shared cache.  A second non-transactional read now caches the
            // still-committed old label while the writer remains uncommitted.
            Label cachedBySecondReader = reader.submit(() -> store.labelForQuad(quad)).get(30, TimeUnit.SECONDS);
            Assertions.assertEquals(LABEL, cachedBySecondReader);

            transactional.commit();

            // The commit must not leave the reader's now-stale cache entry in place
            Assertions.assertEquals(replacement, store.labelForQuad(quad));
        } finally {
            reader.shutdownNow();
        }
    }

    @Test
    void givenBulkWriteOutsideTransaction_whenRepeated_thenRepeatIsSkippedViaCache() throws Exception {
        try (LabelsStore store = create()) {
            Quad quad = SSE.parseQuad("(:g :s :p :o)");

            store.addAll(List.of(quad), LABEL);
            store.addAll(List.of(quad), LABEL);

            Assertions.assertEquals(LABEL, store.labelForQuad(quad));
            Assertions.assertEquals(1L, store.getMetrics().get(LabelsStore.METRIC_LABEL_CACHE_NO_OPS));
            Assertions.assertEquals(1L, store.getMetrics().get(LabelsStore.METRIC_LABEL_WRITES));
        }
    }

    @Test
    void givenAbortedBulkWrite_whenLaterTransactionCommits_thenCacheIsKept() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            Quad aborted = SSE.parseQuad("(:g :s1 :p :o)");
            Quad kept = SSE.parseQuad("(:g :s2 :p :o)");

            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(aborted), LABEL);
            transactional.abort();

            // add() caches what it writes; this commit must not clear the cache on account of the aborted bulk write
            transactional.begin(TxnType.WRITE);
            store.add(kept, LABEL);
            transactional.commit();

            store.addAll(List.of(kept), LABEL);

            Assertions.assertNotEquals(LABEL, store.labelForQuad(aborted));
            Assertions.assertEquals(1L, store.getMetrics().get(LabelsStore.METRIC_LABEL_CACHE_NO_OPS));
            Assertions.assertEquals(2L, store.getMetrics().get(LabelsStore.METRIC_LABEL_WRITES));
        }
    }

    /**
     * A store whose database lookups can be paused after reading, so a test can hold a cache load in flight
     */
    private static final class PausableLookupStore extends DictionaryLabelStoreRocksDB {
        private volatile CountDownLatch loaded = new CountDownLatch(0);
        private volatile CountDownLatch release = new CountDownLatch(0);

        PausableLookupStore(File dbPath, StoreFmt storeFmt) throws IOException, RocksDBException {
            super(dbPath, storeFmt);
        }

        void pauseNextLookup() {
            this.loaded = new CountDownLatch(1);
            this.release = new CountDownLatch(1);
        }

        @Override
        protected Label labelForQuadInternal(Quad quad) {
            Label label = super.labelForQuadInternal(quad);
            this.loaded.countDown();
            try {
                if (!this.release.await(30, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Lookup was never released");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return label;
        }
    }

    @Test
    void givenCacheLoadInFlightDuringBulkRelabel_whenRelabelCommits_thenStaleLabelIsNotCached() throws Exception {
        ExecutorService reader = Executors.newSingleThreadExecutor();
        try (PausableLookupStore store = new PausableLookupStore(Files.createTempDirectory("rocks").toFile(),
                                                                 new StoreFmtByHash(HasherUtil.createXX128Hasher()))) {
            Transactional transactional = store.getTransactional();
            Quad quad = SSE.parseQuad("(:g :s :p :o)");
            Label replacement = Label.fromText("replacement");

            // A bulk write of a new quad leaves it uncached once committed
            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(quad), LABEL);
            transactional.commit();

            // A reader starts loading the quad's label and reads the committed (soon to be old) label from RocksDB
            store.pauseNextLookup();
            Future<Label> seenByReader = reader.submit(() -> store.labelForQuad(quad));
            Assertions.assertTrue(store.loaded.await(30, TimeUnit.SECONDS));

            // While that load is still in flight a bulk write relabels the quad and commits, clearing the cache
            transactional.begin(TxnType.WRITE);
            store.addAll(List.of(quad), replacement);
            transactional.commit();

            // The reader's load then completes; it read before the commit so may return the old label...
            store.release.countDown();
            Assertions.assertEquals(LABEL, seenByReader.get(30, TimeUnit.SECONDS));

            // ...but it must not leave that old label cached for everyone else
            Assertions.assertEquals(replacement, store.labelForQuad(quad));
        } finally {
            reader.shutdownNow();
        }
    }
}
