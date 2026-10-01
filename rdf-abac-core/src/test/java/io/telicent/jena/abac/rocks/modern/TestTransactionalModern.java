package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import io.telicent.jena.abac.AbstractionTransactionalTests;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;

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
    public void givenAbortedWrite_whenRetryingSameLabel_thenRetryIsCommitted() throws Exception {
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
    public void givenDuplicateQuads_whenAddingBatch_thenWritesEachQuadOnce() throws Exception {
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
    public void givenExistingQuad_whenBatchChangesLabel_thenNewLabelWins() throws Exception {
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
}
