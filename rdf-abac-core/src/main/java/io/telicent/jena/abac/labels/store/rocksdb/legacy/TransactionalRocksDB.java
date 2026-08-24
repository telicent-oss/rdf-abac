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

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.query.TxnType;
import org.apache.jena.sparql.JenaTransactionException;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.rocksdb.*;

/**
 * An implementation of {@link Transactional} used by the {@code RocksDB} label
 * store.
 * <p>
 * Within a transaction, a {@code put()} is appended to a {@link WriteBatch} At the
 * end of a transaction, on {@code commit()}, the current write batch is flushed to
 * the database, and then cleared for re-use.
 * <p>
 * As a consequences, the WriteBatch is not visible to "read" operations such as
 * {@link LabelsStore#labelForTriple}. In other words, there is no read-after-write
 * within write transaction.
 */
@SuppressWarnings({ "deprecation", "java:S106", "java:S125", "java:S1117", "java:S5164" })
public class TransactionalRocksDB implements Transactional {
    private record WriteResources(WriteBatch batch, WriteOptions options) {
        void close() {
            batch.close();
            options.close();
        }
    }

    private final RocksDB db;
    private final Cache<Quad, Label> cache;

    // Type of the transaction.
    private final ThreadLocal<Optional<TxnType>> txnType = ThreadLocal.withInitial(Optional::empty);
    // Current mode of the transaction.
    // This is Optional.empty outside a transaction.
    private final ThreadLocal<Optional<ReadWrite>> txnMode = ThreadLocal.withInitial(Optional::empty);

    // Fixed for the lifetime of a transaction
    private Optional<TxnType> getThisTxnType() { return this.txnType.get(); }
    private void setThisTxnType(Optional<TxnType> txnType) {this.txnType.set(txnType); }

    private Optional<ReadWrite> getThisTxnMode() { return this.txnMode.get(); }
    private void setThisTxnMode(Optional<ReadWrite> txnMode) {this.txnMode.set(txnMode); }

    private final ThreadLocal<WriteBatch> writeBatch = new ThreadLocal<>();
    private final ThreadLocal<WriteOptions> writeOptions = new ThreadLocal<>();
    // Tracks live per-thread JNI resources so close()/restore() can drain them if a thread never reaches end().
    private final ConcurrentMap<Thread, WriteResources> liveWriteResources = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    private void startWriteResources() {
        if ( closed ) {
            throw new JenaTransactionException("Transactional RocksDB is closed (store closed or restored?)");
        }
        WriteBatch batch = writeBatch.get();
        WriteOptions options = writeOptions.get();
        if ( (batch == null) != (options == null) ) {
            throw new JenaTransactionException("Transactional RocksDB has inconsistent write resources for the current thread");
        }
        if ( batch == null ) {
            WriteResources resources = new WriteResources(new WriteBatch(), new WriteOptions());
            writeBatch.set(resources.batch());
            writeOptions.set(resources.options());

            WriteResources previous = liveWriteResources.put(Thread.currentThread(), resources);
            if ( previous != null ) {
                previous.close();
            }
        }
    }

    private WriteBatch getThisWriteBatch() {
        WriteBatch batch = writeBatch.get();
        if ( batch == null || !batch.isOwningHandle() ) {
            throw new JenaTransactionException("Transactional RocksDB write batch is closed (store closed or restored?)");
        }
        return batch;
    }

    private WriteOptions getThisWriteOptions() {
        WriteOptions options = writeOptions.get();
        if ( options == null || !options.isOwningHandle() ) {
            throw new JenaTransactionException("Transactional RocksDB write options are closed (store closed or restored?)");
        }
        return options;
    }

    private static Throwable mergeFailure(Throwable priorFailure, Throwable nextFailure) {
        if ( priorFailure != null ) {
            priorFailure.addSuppressed(nextFailure);
            return priorFailure;
        }
        return nextFailure;
    }

    private static void throwFailure(Throwable failure) {
        if ( failure == null ) {
            return;
        }
        if ( failure instanceof RuntimeException runtimeException ) {
            throw runtimeException;
        }
        if ( failure instanceof Error error ) {
            throw error;
        }
        throw new RuntimeException(failure);
    }

    private void finishAutoTransaction(boolean abortIfActive, Throwable priorFailure) {
        Throwable cleanupFailure = null;
        if ( abortIfActive && isInTransaction() ) {
            try {
                abort();
            } catch (Throwable t) {
                cleanupFailure = mergeFailure(cleanupFailure, t);
            }
        }
        try {
            end();
        } catch (Throwable t) {
            cleanupFailure = mergeFailure(cleanupFailure, t);
        }
        if ( priorFailure != null ) {
            if ( cleanupFailure != null ) {
                mergeFailure(priorFailure, cleanupFailure);
            }
            return;
        }
        throwFailure(cleanupFailure);
    }

    // Development helper.
    private static final boolean TRACE = false;
    private static final PrintStream out = System.out;
    private static void trace(String fmt, Object... args) {
        out.print("RocksLabels: ");
        out.printf(fmt, args);
        if ( ! fmt.endsWith("\n") )
            out.println();
    }
    // ----

    /*package*/ TransactionalRocksDB(RocksDB db, Cache<Quad, Label> cache) {
        this.db = db;
        this.cache = cache;
    }

    @Override
    public void begin(TxnType txnType) {
        if ( TRACE ) trace("begin(%s)", txnType);
        Objects.requireNonNull(txnType);
        if ( closed ) {
            throw new JenaTransactionException("Transactional RocksDB is closed (store closed or restored?)");
        }
        Optional<TxnType> currentTxnType = getThisTxnType();
        if (currentTxnType.isPresent())
            throw new JenaTransactionException("Transactional RocksDB begin() called within an existing "+currentTxnType.get()+" transaction");
        if ( txnType == TxnType.READ_COMMITTED_PROMOTE )
            throw new JenaTransactionException("Transactional RocksDB begin() : not supported: READ_COMMITTED_PROMOTE");
        setThisTxnType(Optional.of(txnType));
        ReadWrite mode = TxnType.initial(txnType);
        setThisTxnMode(Optional.of(mode));
        if ( mode == ReadWrite.WRITE ) {
            startWriteResources();
        }
    }

    @Override
    public void begin() {
        if ( TRACE ) trace("begin()");
        begin(TxnType.WRITE);
    }

    @Override
    @SuppressWarnings("java:S3516")
    public boolean promote(Promote promote) {
        if ( TRACE ) trace("promote(%s)",promote);
        Optional<ReadWrite> optReadWrite = getThisTxnMode();
        if ( optReadWrite.isEmpty() )
            throw new JenaTransactionException("Transactional RocksDB promote(): not in a transaction");
        if ( optReadWrite.get() == ReadWrite.WRITE )
            // Already a writer
            return true;

        switch(promote) {
            case ISOLATED, READ_COMMITTED :
                break;
            default : throw new JenaTransactionException("Transactional RocksDB promote(): bad promote type: "+promote);
        }
        // Convert to write mode.
        startWriteResources();
        setThisTxnMode(Optional.of(ReadWrite.WRITE));
        setThisTxnType(Optional.of(TxnType.WRITE));
        // It is the surrounding dataset that decides where promote is possible.
        return true;
    }

    @Override
    public void commit() {
        if ( TRACE ) trace("commit()");
        try {
            if (getThisTxnType().isEmpty())
                throw new JenaTransactionException("Transactional RocksDB commit() called without a transaction");
            getThisTxnMode().ifPresent(value -> {
                if (value == ReadWrite.WRITE) {
                    try {
                        db.write(getThisWriteOptions(), getThisWriteBatch());
                    } catch (RocksDBException e) {
                        throw new JenaTransactionException("Could not flush write batch to RocksDB label store", e);
                    }
                }
            });
        } finally {
            clearThreadLocals();
        }
    }

    @Override
    public void abort() {
        if ( TRACE ) trace("abort()");
        try {
            if (getThisTxnType().isEmpty())
                throw new JenaTransactionException("Transactional RocksDB abort() called without a transaction");
            getThisTxnMode().ifPresent(value -> {
                if (value == ReadWrite.WRITE) {
                    // Have to clear the label cache after an abort() otherwise aborted label changes will leak outside the
                    // aborted transaction
                    cache.clear();
                }
            });
        } finally {
            clearThreadLocals();
        }
    }

    @Override
    public void end() {
        if ( TRACE ) trace("end()");
        if ( getThisTxnMode().filter(value -> value == ReadWrite.WRITE).isPresent() ) {
            // Jena API contract says that if end() is called without a corresponding commit() then must abort() the
            // transaction
            this.abort();
            return;
        }
        clearThreadLocals();
    }

    private void clearThreadLocals() {
        WriteResources resources = liveWriteResources.remove(Thread.currentThread());
        WriteBatch batch = writeBatch.get();
        WriteOptions options = writeOptions.get();

        txnMode.remove();
        txnType.remove();
        writeBatch.remove();
        writeOptions.remove();

        if ( resources != null ) {
            resources.close();
            return;
        }
        if ( batch != null ) {
            batch.close();
        }
        if ( options != null ) {
            options.close();
        }
    }

    void close() {
        closed = true;
        boolean clearCache = getThisTxnMode().filter(value -> value == ReadWrite.WRITE).isPresent();
        if ( isInTransaction() ) {
            Labels.LOG.warn("Transactional RocksDB close() called while the current thread is in a transaction; forcing cleanup");
        }
        clearThreadLocals();
        boolean drainedWriteResources = liveWriteResources.values().removeIf(resources -> {
            resources.close();
            return true;
        });
        if ( clearCache || drainedWriteResources ) {
            cache.clear();
        }
    }

    @Override
    public ReadWrite transactionMode() {
        return getThisTxnMode().orElse(null);
    }

    @Override
    public TxnType transactionType() {
        return getThisTxnType().orElse(null);
    }

    @Override
    public boolean isInTransaction() {
        return getThisTxnMode().isPresent();
    }

//    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void execute(Runnable action) {
        if ( TRACE ) trace("execute");
        if (isInTransaction()) {
            action.run();
        } else {
            boolean completedTransaction = false;
            Throwable failure = null;
            begin();
            try {
                action.run();
                commit();
                completedTransaction = true;
            } catch (Throwable t) {
                failure = t;
                throw t;
            } finally {
                finishAutoTransaction(!completedTransaction, failure);
            }
        }
    }

    public void merge(ColumnFamilyHandle columnFamilyHandle, ByteBuffer key, ByteBuffer value) {
        rocksOperation(AbstractWriteBatch::merge, columnFamilyHandle, key, value);
    }

    public void put(ColumnFamilyHandle columnFamilyHandle, ByteBuffer key, ByteBuffer value) {
        rocksOperation(AbstractWriteBatch::put, columnFamilyHandle, key, value);
    }

    public void delete(ColumnFamilyHandle columnFamilyHandle, ByteBuffer key) {
        rocksOperation((batch, cfh, k, v) -> batch.delete(cfh, k), columnFamilyHandle, key, ByteBuffer.allocate(0));
    }

    public interface WriteOperation {
        void write(WriteBatch writeBatch, ColumnFamilyHandle columnFamilyHandle, byte[] key, byte[] value)
            throws RocksDBException;
    }

    private void rocksOperation(WriteOperation op, ColumnFamilyHandle columnFamilyHandle, ByteBuffer key, ByteBuffer value) {
        boolean transactionExists = getThisTxnType().isPresent();
        boolean completedTransaction = false;
        Throwable failure = null;
        if (!transactionExists) {
            begin(TxnType.WRITE);
        }
        try {
            Optional<ReadWrite> txnMode = getThisTxnMode();
            if (txnMode.isPresent() && txnMode.get() == ReadWrite.READ) {
                Optional<TxnType> txnType = getThisTxnType();
                if (txnType.isEmpty()) {
                    throw new JenaTransactionException("Read transaction is missing its transaction type");
                }
                TxnType currentTxnType = txnType.get();
                switch (currentTxnType) {
                    case READ -> throw new JenaTransactionException("Cannot promote READ transaction to write");
                    case READ_PROMOTE -> promote(Promote.ISOLATED);
                    case READ_COMMITTED_PROMOTE ->
                            throw new JenaTransactionException("Promoting READ_COMMITTED_PROMOTE transaction to write is not supported");
                    default -> throw new JenaTransactionException("Unexpected transaction type: " + currentTxnType);
                }
            }

            byte[] k = new byte[key.limit() - key.position()];
            key.get(k);
            byte[] v = new byte[value.limit() - value.position()];
            value.get(v);
            op.write(getThisWriteBatch(), columnFamilyHandle, k, v);
            if (!transactionExists) {
                commit();
                completedTransaction = true;
            }
        } catch (RocksDBException e) {
            failure = new JenaTransactionException("Could not write to write batch for RocksDB label store", e);
            throw (JenaTransactionException) failure;
        } catch (Throwable t) {
            failure = t;
            throw t;
        } finally {
            if (!transactionExists) {
                finishAutoTransaction(!completedTransaction, failure);
            }
        }
    }

}
