package io.telicent.jena.abac.rocks.modern;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsException;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.jena.abac.labels.StoreFmt;
import io.telicent.jena.abac.labels.store.rocksdb.modern.DictionaryLabelStoreRocksDB;
import io.telicent.jena.abac.rocks.AbstractTestLabelMatchRocks;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.sse.SSE;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

public class AbstractTestLabelMatchModernRocks extends AbstractTestLabelMatchRocks {

    private static final Node s = SSE.parseNode(":s");
    private static final Node p = SSE.parseNode(":p");
    private static final Node o = SSE.parseNode(":o");

    @Override
    protected LabelsStore createLabelsStore(StoreFmt storeFmt) {
        try {
            dbDirectory = Files.createTempDirectory("tmp" + storeFmt.getClass()).toFile();
            return new DictionaryLabelStoreRocksDB(dbDirectory, storeFmt);
        } catch (RocksDBException | IOException e) {
            throw new RuntimeException("Unable to create RocksDB label store", e);
        }
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void remove_existingLabel_removesIt(StoreFmt storeFmt) throws Exception {
        try (LabelsStore store = createLabelsStore(storeFmt)) {
            final Triple triple = Triple.create(s, p, o);
            final Quad quad = Quad.create(Quad.defaultGraphIRI, triple);
            final Label label = Label.fromText("sensitive");
            store.add(quad, label);
            assertEquals(label, store.labelForQuad(quad));

            store.remove(quad);

            assertNull(store.labelForQuad(quad));
        }
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void remove_nonExistentLabel_isNoOp(StoreFmt storeFmt) throws Exception {
        try (LabelsStore store = createLabelsStore(storeFmt)) {
            final Triple triple = Triple.create(s, p, o);
            final Quad quad = Quad.create(Quad.defaultGraphIRI, triple);
            assertDoesNotThrow(() -> store.remove(quad));
            assertNull(store.labelForQuad(quad));
        }
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void remove_wildcardQuad_throwsLabelsException(StoreFmt storeFmt) throws Exception {
        try (LabelsStore store = createLabelsStore(storeFmt)) {
            final Quad wildcard = Quad.create(Quad.defaultGraphIRI, Node.ANY, p, o);
            assertThrows(LabelsException.class, () -> store.remove(wildcard));
        }
    }

    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFormat")
    public void remove_thenReAdd_labelIsVisibleAgain(StoreFmt storeFmt) throws Exception {
        try (LabelsStore store = createLabelsStore(storeFmt)) {
            final Triple triple = Triple.create(s, p, o);
            final Quad quad = Quad.create(Quad.defaultGraphIRI, triple);
            final Label label = Label.fromText("public");
            store.add(quad, label);
            store.remove(quad);
            assertNull(store.labelForQuad(quad));

            store.add(quad, label);
            assertEquals(label, store.labelForQuad(quad));
        }
    }
}
