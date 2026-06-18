package io.telicent.jena.abac;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.JenaTransactionException;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.system.Txn;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation")
public abstract class AbstractionTransactionalTests {

    private static final Triple TRIPLE = SSE.parseTriple("(:s :p :o)");
    public static final Label LABEL = Label.fromText("public");

    /**
     * Creates an empty fresh instance of the store for testing
     *
     * @return Fresh empty store
     */
    protected abstract LabelsStore create();

    /**
     * Whether the store under test actively enforces read-only transactions by rejecting writes.
     * <p>
     * Only stores with genuine read/write transaction awareness (currently the modern RocksDB store) do this; the
     * in-memory and legacy stores treat every transaction as a write and so never reject writes.  Such stores
     * override this to enable the read-only write rejection test.
     * </p>
     *
     * @return True if writes in a read-only transaction are rejected, false otherwise
     */
    protected boolean enforcesReadOnlyTransactions() {
        return false;
    }

    @Test
    public void givenStoreTransactional_whenCallingPlainBegin_thenWriteTransaction() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin();

            // Then
            Assertions.assertEquals(ReadWrite.WRITE, transactional.transactionMode());
            Assertions.assertEquals(TxnType.WRITE, transactional.transactionType());
        }
    }

    @Test
    public void givenStoreTransactional_whenCallingCommit_thenCallingEndAfterwardsIsOk() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.WRITE);
            store.add(TRIPLE, LABEL);
            transactional.commit();

            // Then
            transactional.end();
        }
    }

    @Test
    public void givenStoreTransactional_whenExecutingWrite_thenOk() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When and Then
            Txn.executeWrite(store.getTransactional(),
                             () -> store.add(TRIPLE,
                                             LABEL));
        }
    }

    @Test
    public void givenStoreTransactional_whenExecutingRead_thenOk() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When and Then
            Txn.executeRead(store.getTransactional(),
                            () -> Assertions.assertNull(store.labelForTriple(TRIPLE)));
        }
    }

    @Test
    public void givenStoreTransactional_whenPromotingFromReadPromote_thenOk() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.READ_PROMOTE);
            Assertions.assertNull(store.labelForTriple(TRIPLE));
            Assertions.assertTrue(transactional.promote());
            store.add(TRIPLE, LABEL);
            transactional.commit();

            // Then
            Assertions.assertEquals(LABEL, store.labelForTriple(TRIPLE));
        }
    }

    @Test
    public void givenStoreTransactional_whenPromotingFromPlainRead_thenNotPromoted() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.READ);
            Assumptions.assumeTrue(transactional.transactionType() == TxnType.READ);
            Assertions.assertNull(store.labelForTriple(TRIPLE));

            // Then
            Assertions.assertFalse(transactional.promote());
            Assertions.assertNotEquals(TxnType.WRITE, transactional.transactionType());
            transactional.commit();
        }
    }

    @Test
    public void givenStoreTransactional_whenWritingInPlainRead_thenFails() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            // Only stores that actively enforce read-only transactions reject writes; the in-memory and legacy stores
            // treat every transaction as a write, so this assertion only applies where enforcement exists.
            Assumptions.assumeTrue(enforcesReadOnlyTransactions());
            transactional.begin(TxnType.READ);

            Assertions.assertThrows(JenaTransactionException.class, () -> store.add(TRIPLE, LABEL));

            transactional.end();
        }
    }

    @Test
    public void givenStoreTransactional_whenPromotingFromWrite_thenOk() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.WRITE);
            Assertions.assertNull(store.labelForTriple(TRIPLE));
            Assertions.assertTrue(transactional.promote());
            store.add(TRIPLE, LABEL);
            transactional.commit();

            // Then
            Assertions.assertEquals(LABEL, store.labelForTriple(TRIPLE));
        }
    }

    @Test
    public void givenStoreTransactional_whenPromotingByModeFromReadPromote_thenOk() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.READ_PROMOTE);
            Assertions.assertNull(store.labelForTriple(TRIPLE));
            Assertions.assertTrue(transactional.promote(Transactional.Promote.ISOLATED));
            store.add(TRIPLE, LABEL);
            transactional.commit();

            // Then
            Assertions.assertEquals(LABEL, store.labelForTriple(TRIPLE));
        }
    }

    @Test
    public void givenStoreTransactional_whenExecutingWriteInsideReadPromote_thenAutoPromotes() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.READ_PROMOTE);
            Assertions.assertEquals(ReadWrite.READ, transactional.transactionMode());

            Txn.executeWrite(transactional, () -> {
                Assertions.assertEquals(ReadWrite.WRITE, transactional.transactionMode());
                store.add(TRIPLE, LABEL);
            });

            transactional.commit();

            Assertions.assertEquals(LABEL, store.labelForTriple(TRIPLE));
        }
    }

    @Test
    public void givenStoreTransactional_whenPromotingByModeFromPlainRead_thenOk() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.READ);
            Assumptions.assumeTrue(transactional.transactionType() == TxnType.READ);
            Assertions.assertNull(store.labelForTriple(TRIPLE));

            // Then
            Assertions.assertTrue(transactional.promote(Transactional.Promote.ISOLATED));
            Assertions.assertEquals(ReadWrite.WRITE, transactional.transactionMode());
            // NB - Bug in Jena code, promote() only updates transactionMode() and not transactionType() in some
            //      implementations of Transactional
            //Assertions.assertEquals(TxnType.WRITE, transactional.transactionType());
            transactional.commit();
        }
    }

    @Test
    public void givenStoreTransactional_whenPromotingByModeFromWrite_thenOk() throws Exception {
        // Given
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.WRITE);
            Assertions.assertNull(store.labelForTriple(TRIPLE));
            Assertions.assertTrue(transactional.promote(Transactional.Promote.ISOLATED));
            store.add(TRIPLE, LABEL);
            transactional.commit();

            // Then
            Assertions.assertEquals(LABEL, store.labelForTriple(TRIPLE));
        }
    }

    @Test
    public void givenStoreTransactional_whenAborting_thenChangesNotCommitted() throws Exception {
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.WRITE);
            store.add(TRIPLE, LABEL);
            transactional.abort();

            // Then
            transactional.begin(TxnType.READ);
            Assertions.assertNull(store.labelForTriple(TRIPLE));
            transactional.end();
        }
    }

    @Test
    public void givenStoreTransactional_whenEndingWithoutCommit_thenChangesNotCommitted() throws Exception {
        try (LabelsStore store = create()) {
            // When
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.WRITE);
            store.add(TRIPLE, LABEL);
            transactional.end();

            // Then
            Assertions.assertNull(store.labelForTriple(TRIPLE));
        }
    }

    @Test
    public void givenStoreTransactional_whenBeginningMoreThanOnce_thenFails() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.WRITE);
            Assertions.assertThrows(JenaTransactionException.class, () -> transactional.begin(TxnType.WRITE));
        }
    }

    @Test
    public void givenStoreTransactional_whenCommittingOutsideATransaction_thenFails() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            Assertions.assertThrows(JenaTransactionException.class, transactional::commit);
        }
    }

    @Test
    public void givenStoreTransactional_whenAbortingOutsideATransaction_thenFails() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            Assertions.assertThrows(JenaTransactionException.class, transactional::abort);
        }
    }

    @Test
    public void givenStoreTransactional_whenPromotingOutsideATransaction_thenFails() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            Assertions.assertThrows(JenaTransactionException.class, transactional::promote);
        }
    }

    @Test
    public void givenStoreTransactional_whenPromotingByModeOutsideATransaction_thenFails() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            Assertions.assertThrows(JenaTransactionException.class,
                                    () -> transactional.promote(Transactional.Promote.ISOLATED));
        }
    }

    @Test
    public void givenStoreTransactional_whenCommittingTwice_thenFails() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            transactional.begin(TxnType.WRITE);
            transactional.commit();
            Assertions.assertThrows(JenaTransactionException.class, transactional::commit);
        }
    }

    @Test
    public void givenStoreTransactional_whenEndingOutsideATransaction_thenNoOp() throws Exception {
        try (LabelsStore store = create()) {
            Transactional transactional = store.getTransactional();
            transactional.end();
        }
    }
}
