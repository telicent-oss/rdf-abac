package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.AbstractionTransactionalTests;
import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.RocksDBHelper;
import io.telicent.jena.abac.labels.store.rocksdb.legacy.TransactionalRocksDB;
import org.apache.jena.graph.Triple;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.JenaTransactionException;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.rocksdb.WriteBatch;
import org.rocksdb.WriteOptions;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings({"java:S5778","java:S5786"})
public class TestTransactionalLegacy extends AbstractionTransactionalTests {
    private static final Triple TRIPLE = SSE.parseTriple("(:s :p :o)");
    private static final Label LABEL = Label.fromText("public");

    @Override
    protected LabelsStore create() {
        try {
            return createStore(Files.createTempDirectory("rocks"));
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void givenLegacyTransactional_whenCommitThenEnd_thenThreadLocalWriteOptionsAreClosedAndRemoved() throws Exception {
        try (LabelsStore store = create()) {
            TransactionalRocksDB transactional = (TransactionalRocksDB) store.getTransactional();
            ThreadLocal<WriteBatch> writeBatchThreadLocal = writeBatchThreadLocal(transactional);
            ThreadLocal<WriteOptions> writeOptionsThreadLocal = writeOptionsThreadLocal(transactional);

            transactional.begin(TxnType.WRITE);
            WriteBatch batch = writeBatchThreadLocal.get();
            WriteOptions options = writeOptionsThreadLocal.get();

            store.add(TRIPLE, LABEL);
            transactional.commit();
            transactional.end();

            assertWriteResourcesClosedAndRemoved(writeBatchThreadLocal, batch, writeOptionsThreadLocal, options);
        }
    }

    @Test
    public void givenLegacyTransactional_whenAbortThenEnd_thenThreadLocalWriteOptionsAreClosedAndRemoved() throws Exception {
        try (LabelsStore store = create()) {
            TransactionalRocksDB transactional = (TransactionalRocksDB) store.getTransactional();
            ThreadLocal<WriteBatch> writeBatchThreadLocal = writeBatchThreadLocal(transactional);
            ThreadLocal<WriteOptions> writeOptionsThreadLocal = writeOptionsThreadLocal(transactional);

            transactional.begin(TxnType.WRITE);
            WriteBatch batch = writeBatchThreadLocal.get();
            WriteOptions options = writeOptionsThreadLocal.get();

            store.add(TRIPLE, LABEL);
            transactional.abort();
            transactional.end();

            assertWriteResourcesClosedAndRemoved(writeBatchThreadLocal, batch, writeOptionsThreadLocal, options);
        }
    }

    @Test
    public void givenConcurrentReadTransaction_whenLegacyWriterCommits_thenWritePersistsToDisk() throws Exception {
        Path dbPath = Files.createTempDirectory("rocks");
        try (LabelsStore store = createStore(dbPath)) {
            Transactional transactional = store.getTransactional();
            CountDownLatch writerReady = new CountDownLatch(1);
            CountDownLatch readerDone = new CountDownLatch(1);
            AtomicReference<Throwable> writerFailure = new AtomicReference<>();
            AtomicReference<Throwable> readerFailure = new AtomicReference<>();

            Thread writer = new Thread(() -> {
                try {
                    transactional.begin(TxnType.WRITE);
                    store.add(TRIPLE, LABEL);
                    writerReady.countDown();
                    Assertions.assertTrue(readerDone.await(5, TimeUnit.SECONDS));
                    transactional.commit();
                    transactional.end();
                } catch (Throwable t) {
                    writerFailure.set(t);
                }
            });

            Thread reader = new Thread(() -> {
                try {
                    Assertions.assertTrue(writerReady.await(5, TimeUnit.SECONDS));
                    transactional.begin(TxnType.READ);
                    transactional.commit();
                    transactional.end();
                    readerDone.countDown();
                } catch (Throwable t) {
                    readerFailure.set(t);
                }
            });

            writer.start();
            reader.start();
            writer.join(TimeUnit.SECONDS.toMillis(5));
            reader.join(TimeUnit.SECONDS.toMillis(5));

            Assertions.assertFalse(writer.isAlive(), "writer thread did not finish");
            Assertions.assertFalse(reader.isAlive(), "reader thread did not finish");
            if (writerFailure.get() != null) {
                throw new AssertionError("writer thread failed", writerFailure.get());
            }
            if (readerFailure.get() != null) {
                throw new AssertionError("reader thread failed", readerFailure.get());
            }
        }

        try (LabelsStore reopenedStore = createStore(dbPath)) {
            Assertions.assertEquals(LABEL, reopenedStore.labelForTriple(TRIPLE));
        }
    }

    @Test
    public void givenWriteResourcesOnAnotherThread_whenStoreClosed_thenTrackedResourcesAreDrained() throws Exception {
        try (LabelsStore store = create()) {
            TransactionalRocksDB transactional = (TransactionalRocksDB) store.getTransactional();
            ThreadLocal<WriteBatch> writeBatchThreadLocal = writeBatchThreadLocal(transactional);
            ThreadLocal<WriteOptions> writeOptionsThreadLocal = writeOptionsThreadLocal(transactional);
            CountDownLatch writerReady = new CountDownLatch(1);
            CountDownLatch allowExit = new CountDownLatch(1);
            AtomicReference<WriteBatch> batchRef = new AtomicReference<>();
            AtomicReference<WriteOptions> optionsRef = new AtomicReference<>();
            AtomicReference<Throwable> writerFailure = new AtomicReference<>();

            Thread writer = new Thread(() -> {
                try {
                    transactional.begin(TxnType.WRITE);
                    batchRef.set(writeBatchThreadLocal.get());
                    optionsRef.set(writeOptionsThreadLocal.get());
                    writerReady.countDown();
                    Assertions.assertTrue(allowExit.await(5, TimeUnit.SECONDS));
                } catch (Throwable t) {
                    writerFailure.set(t);
                }
            });

            writer.start();
            Assertions.assertTrue(writerReady.await(5, TimeUnit.SECONDS));

            WriteBatch batch = batchRef.get();
            WriteOptions options = optionsRef.get();
            Assertions.assertNotNull(batch);
            Assertions.assertNotNull(options);

            store.close();
            allowExit.countDown();
            writer.join(TimeUnit.SECONDS.toMillis(5));

            Assertions.assertFalse(writer.isAlive(), "writer thread did not finish");
            if (writerFailure.get() != null) {
                throw new AssertionError("writer thread failed", writerFailure.get());
            }
            Assertions.assertFalse(batch.isOwningHandle());
            Assertions.assertFalse(options.isOwningHandle());
        }
    }

    @Test
    public void givenWriteResourcesClosedFromAnotherThread_whenWriterCommits_thenFailsCleanly() throws Exception {
        try (LabelsStore store = create()) {
            TransactionalRocksDB transactional = (TransactionalRocksDB) store.getTransactional();
            CountDownLatch writerReady = new CountDownLatch(1);
            CountDownLatch allowCommit = new CountDownLatch(1);
            AtomicReference<Throwable> writerFailure = new AtomicReference<>();

            Thread writer = new Thread(() -> {
                try {
                    transactional.begin(TxnType.WRITE);
                    store.add(TRIPLE, LABEL);
                    writerReady.countDown();
                    Assertions.assertTrue(allowCommit.await(5, TimeUnit.SECONDS));
                    transactional.commit();
                    transactional.end();
                } catch (Throwable t) {
                    writerFailure.set(t);
                }
            });

            writer.start();
            Assertions.assertTrue(writerReady.await(5, TimeUnit.SECONDS));

            store.close();
            allowCommit.countDown();
            writer.join(TimeUnit.SECONDS.toMillis(5));

            Assertions.assertFalse(writer.isAlive(), "writer thread did not finish");
            Assertions.assertInstanceOf(JenaTransactionException.class, writerFailure.get());
            Assertions.assertTrue(writerFailure.get().getMessage().contains("closed"));
        }
    }

    @Test
    public void givenActiveTransactionOnClosingThread_whenStoreClosed_thenStoreStillCloses() throws Exception {
        try (LabelsStore store = create()) {
            TransactionalRocksDB transactional = (TransactionalRocksDB) store.getTransactional();
            transactional.begin(TxnType.WRITE);

            Assertions.assertDoesNotThrow(store::close);
        }
    }

    @Test
    public void givenStaleTransactionalReference_whenBeginAfterStoreClose_thenFailsBeforeAllocatingWriteResources() throws Exception {
        LegacyLabelsStoreRocksDB store = createStore(Files.createTempDirectory("rocks"));
        try {
            TransactionalRocksDB transactional = (TransactionalRocksDB) store.getTransactional();
            ThreadLocal<WriteBatch> writeBatchThreadLocal = writeBatchThreadLocal(transactional);
            ThreadLocal<WriteOptions> writeOptionsThreadLocal = writeOptionsThreadLocal(transactional);
            Map<?, ?> liveWriteResources = liveWriteResources(transactional);

            store.close();

            JenaTransactionException failure = Assertions.assertThrows(JenaTransactionException.class,
                                                                      () -> transactional.begin(TxnType.WRITE));
            Assertions.assertTrue(failure.getMessage().contains("closed"));
            Assertions.assertNull(writeBatchThreadLocal.get());
            Assertions.assertNull(writeOptionsThreadLocal.get());
            Assertions.assertTrue(liveWriteResources.isEmpty());
        } finally {
            store.close();
        }
    }

    @Test
    public void givenReadPromoteTransactionClosedFromAnotherThread_whenPromoteAfterClose_thenFailsBeforeAllocatingWriteResources() throws Exception {
        try (LegacyLabelsStoreRocksDB store = createStore(Files.createTempDirectory("rocks"))) {
            TransactionalRocksDB transactional = (TransactionalRocksDB) store.getTransactional();
            ThreadLocal<WriteBatch> writeBatchThreadLocal = writeBatchThreadLocal(transactional);
            ThreadLocal<WriteOptions> writeOptionsThreadLocal = writeOptionsThreadLocal(transactional);
            Map<?, ?> liveWriteResources = liveWriteResources(transactional);
            CountDownLatch readerReady = new CountDownLatch(1);
            CountDownLatch allowPromote = new CountDownLatch(1);
            AtomicReference<Throwable> promoteFailure = new AtomicReference<>();
            AtomicReference<WriteBatch> batchAfterPromote = new AtomicReference<>();
            AtomicReference<WriteOptions> optionsAfterPromote = new AtomicReference<>();
            AtomicReference<ReadWrite> modeAfterPromote = new AtomicReference<>();
            AtomicReference<TxnType> typeAfterPromote = new AtomicReference<>();

            Thread reader = new Thread(() -> {
                try {
                    transactional.begin(TxnType.READ_PROMOTE);
                    Assertions.assertEquals(ReadWrite.READ, transactional.transactionMode());
                    readerReady.countDown();
                    Assertions.assertTrue(allowPromote.await(5, TimeUnit.SECONDS));
                    transactional.promote(Transactional.Promote.ISOLATED);
                } catch (Throwable t) {
                    promoteFailure.set(t);
                } finally {
                    modeAfterPromote.set(transactional.transactionMode());
                    typeAfterPromote.set(transactional.transactionType());
                    batchAfterPromote.set(writeBatchThreadLocal.get());
                    optionsAfterPromote.set(writeOptionsThreadLocal.get());
                }
            });

            reader.start();
            Assertions.assertTrue(readerReady.await(5, TimeUnit.SECONDS));

            store.close();
            allowPromote.countDown();
            reader.join(TimeUnit.SECONDS.toMillis(5));

            Assertions.assertFalse(reader.isAlive(), "reader thread did not finish");
            Assertions.assertInstanceOf(JenaTransactionException.class, promoteFailure.get());
            Assertions.assertTrue(promoteFailure.get().getMessage().contains("closed"));
            Assertions.assertEquals(ReadWrite.READ, modeAfterPromote.get());
            Assertions.assertEquals(TxnType.READ_PROMOTE, typeAfterPromote.get());
            Assertions.assertNull(batchAfterPromote.get());
            Assertions.assertNull(optionsAfterPromote.get());
            Assertions.assertTrue(liveWriteResources.isEmpty());
        }
    }

    @Test
    public void givenExecuteAutoTransaction_whenActionFails_thenOriginalExceptionIsPreservedAndWriteIsAborted() throws Exception {
        Path dbPath = Files.createTempDirectory("rocks");
        try (LabelsStore store = createStore(dbPath)) {
            TransactionalRocksDB transactional = (TransactionalRocksDB) store.getTransactional();
            ThreadLocal<Optional<TxnType>> txnTypeThreadLocal = txnTypeThreadLocal(transactional);

            IllegalStateException failure = Assertions.assertThrows(IllegalStateException.class, () -> transactional.execute(() -> {
                store.add(TRIPLE, LABEL);
                txnTypeThreadLocal.remove();
                throw new IllegalStateException("boom");
            }));

            Assertions.assertEquals("boom", failure.getMessage());
            Assertions.assertFalse(transactional.isInTransaction());
            Assertions.assertEquals(1, failure.getSuppressed().length);
            Assertions.assertInstanceOf(JenaTransactionException.class, failure.getSuppressed()[0]);
        }

        try (LabelsStore reopenedStore = createStore(dbPath)) {
            Assertions.assertNull(reopenedStore.labelForTriple(TRIPLE));
        }
    }

    @Test
    public void givenRocksOperationAutoTransaction_whenWriteFails_thenTransactionIsCleanedUp() throws Exception {
        Path dbPath = Files.createTempDirectory("rocks");
        try (LabelsStore store = createStore(dbPath)) {
            TransactionalRocksDB transactional = (TransactionalRocksDB) store.getTransactional();

            Assertions.assertThrows(RuntimeException.class,
                                    () -> transactional.put(null, ByteBuffer.wrap(new byte[]{1}), ByteBuffer.wrap(new byte[]{2})));
            Assertions.assertFalse(transactional.isInTransaction());

            transactional.begin(TxnType.WRITE);
            store.add(TRIPLE, LABEL);
            transactional.commit();
            transactional.end();
        }

        try (LabelsStore reopenedStore = createStore(dbPath)) {
            Assertions.assertEquals(LABEL, reopenedStore.labelForTriple(TRIPLE));
        }
    }

    @SuppressWarnings("unchecked")
    private ThreadLocal<WriteBatch> writeBatchThreadLocal(TransactionalRocksDB transactional) throws ReflectiveOperationException {
        Field field = TransactionalRocksDB.class.getDeclaredField("writeBatch");
        field.setAccessible(true);
        return (ThreadLocal<WriteBatch>) field.get(transactional);
    }

    @SuppressWarnings("unchecked")
    private ThreadLocal<WriteOptions> writeOptionsThreadLocal(TransactionalRocksDB transactional) throws ReflectiveOperationException {
        Field field = TransactionalRocksDB.class.getDeclaredField("writeOptions");
        field.setAccessible(true);
        return (ThreadLocal<WriteOptions>) field.get(transactional);
    }

    @SuppressWarnings("unchecked")
    private ThreadLocal<Optional<TxnType>> txnTypeThreadLocal(TransactionalRocksDB transactional) throws ReflectiveOperationException {
        Field field = TransactionalRocksDB.class.getDeclaredField("txnType");
        field.setAccessible(true);
        return (ThreadLocal<Optional<TxnType>>) field.get(transactional);
    }

    @SuppressWarnings("unchecked")
    private Map<Thread, ?> liveWriteResources(TransactionalRocksDB transactional) throws ReflectiveOperationException {
        Field field = TransactionalRocksDB.class.getDeclaredField("liveWriteResources");
        field.setAccessible(true);
        return (Map<Thread, ?>) field.get(transactional);
    }

    private void assertWriteResourcesClosedAndRemoved(ThreadLocal<WriteBatch> writeBatchThreadLocal, WriteBatch batch,
                                                      ThreadLocal<WriteOptions> writeOptionsThreadLocal, WriteOptions options) {
        try {
            Assertions.assertNotNull(batch);
            Assertions.assertNotNull(options);
            Assertions.assertFalse(batch.isOwningHandle());
            Assertions.assertFalse(options.isOwningHandle());
            Assertions.assertNull(writeBatchThreadLocal.get());
            Assertions.assertNull(writeOptionsThreadLocal.get());
        } finally {
            if (batch != null && batch.isOwningHandle()) {
                batch.close();
            }
            if (options != null && options.isOwningHandle()) {
                options.close();
            }
            writeBatchThreadLocal.remove();
            writeOptionsThreadLocal.remove();
        }
    }

    @SuppressWarnings("deprecation")
    private LegacyLabelsStoreRocksDB createStore(Path dbPath) {
        try {
            return new LegacyLabelsStoreRocksDB(new RocksDBHelper(), dbPath.toFile(),
                                                new StoreFmtByHash(HasherUtil.createXX128Hasher()), null);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
