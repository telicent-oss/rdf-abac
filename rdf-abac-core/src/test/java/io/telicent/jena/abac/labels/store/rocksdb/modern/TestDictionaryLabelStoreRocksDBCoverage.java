package io.telicent.jena.abac.labels.store.rocksdb.modern;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsException;
import io.telicent.jena.abac.labels.StoreFmt;
import io.telicent.jena.abac.labels.StoreFmtByHash;
import io.telicent.jena.abac.labels.StoreFmtByString;
import io.telicent.jena.abac.labels.hashing.HasherUtil;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.ReadWrite;
import org.apache.jena.sparql.JenaTransactionException;
import org.apache.jena.sparql.core.Quad;
import org.apache.jena.sparql.core.Transactional;
import org.apache.jena.query.TxnType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({ "java:S5786", "java:S100", "deprecation" })
class TestDictionaryLabelStoreRocksDBCoverage {

    @TempDir
    Path tempDir;

    @Test
    void constructorRejectsUnsupportedOrMismatchedStoreFormats() throws Exception {
        File unsupportedDir = tempDir.resolve("unsupported-format-check").toFile();
        File dbDir = tempDir.resolve("format-check").toFile();

        assertThrows(IllegalArgumentException.class,
                     () -> new DictionaryLabelStoreRocksDB(unsupportedDir, new StoreFmtByString()));

        StoreFmtByHash initial = new StoreFmtByHash(HasherUtil.createXX128Hasher());
        try (DictionaryLabelStoreRocksDB ignored = new DictionaryLabelStoreRocksDB(dbDir, initial)) {
            // Fresh store records the selected format on first open.
        }

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                                                () -> new DictionaryLabelStoreRocksDB(dbDir,
                                                                                      new StoreFmtByHash(
                                                                                              HasherUtil.createMurmer128Hasher())));
        assertTrue(ex.getMessage().contains("different Store Format"));
    }

    @Test
    void basicStoreMethodsAndAdditionalTransactionBranchesAreCovered() throws Exception {
        try (DictionaryLabelStoreRocksDB store = createStore(tempDir.resolve("basic").toFile())) {
            Quad quad = concreteQuad();
            Label label = Label.fromText("restricted");

            assertTrue(store.isEmpty());
            assertEquals("0", store.getProperties().get("size"));
            assertNull(store.asGraph());
            assertThrows(UnsupportedOperationException.class, () -> store.forEach((q, l) -> fail("Unexpected iteration")));
            assertThrows(LabelsException.class, () -> store.labelForQuad(Quad.ANY));
            assertThrows(LabelsException.class, () -> store.add(Quad.ANY, label));

            Transactional transactional = store.getTransactional();
            transactional.begin(ReadWrite.READ);
            assertEquals(ReadWrite.READ, transactional.transactionMode());
            assertEquals(TxnType.READ, transactional.transactionType());
            assertThrows(JenaTransactionException.class, () -> store.add(quad, label));
            transactional.end();

            transactional.begin((TxnType) null);
            assertEquals(ReadWrite.WRITE, transactional.transactionMode());
            assertEquals(TxnType.WRITE, transactional.transactionType());
            transactional.abort();
            transactional.end();

            transactional.begin(TxnType.READ_COMMITTED_PROMOTE);
            assertTrue(transactional.promote(Transactional.Promote.ISOLATED));
            transactional.commit();
            transactional.end();

            transactional.begin(ReadWrite.WRITE);
            store.add(quad, label);
            transactional.commit();
            transactional.end();

            assertFalse(store.isEmpty());
            assertEquals("1", store.getProperties().get("size"));
            assertEquals(label, store.labelForQuad(quad));

            store.remove(quad);
            assertTrue(store.isEmpty());
            assertNull(store.labelForQuad(quad));
        }
    }

    @Test
    void legacyMigrationHelpersCoverFormattingThresholdAndCorruptionPaths() throws Exception {
        try (DictionaryLabelStoreRocksDB store = createStore(tempDir.resolve("helpers").toFile())) {
            Object migrator = createMigrator(store);
            Class<?> migratorClass = migrator.getClass();

            assertEquals("1,234", invokeStatic(migratorClass, "humanReadableCount",
                                               new Class[] { long.class }, 1_234L));
            assertEquals("100%", invokeStatic(migratorClass, "percentage",
                                              new Class[] { long.class, long.class }, 5L, 5L));
            assertEquals("25.00%", invokeStatic(migratorClass, "percentage",
                                                new Class[] { long.class, long.class }, 1L, 4L));
            assertEquals(Boolean.TRUE, invoke(migrator, "exceedsThreshold",
                                              new Class[] { long.class, long.class, double.class }, 10L, 10L, 0.1d));
            assertEquals(Boolean.TRUE, invoke(migrator, "exceedsThreshold",
                                              new Class[] { long.class, long.class, double.class }, 2L, 10L, 0.2d));
            assertEquals(Boolean.FALSE, invoke(migrator, "exceedsThreshold",
                                               new Class[] { long.class, long.class, double.class }, 1L, 10L, 0.2d));

            StoreFmtByHash targetFormat = new StoreFmtByHash(HasherUtil.createXX128Hasher());
            StoreFmt sourceFormat = new StoreFmtByString();
            StoreFmt.Parser parser = sourceFormat.createParser();
            StoreFmt.Encoder encoder = targetFormat.createEncoder();
            ByteBuffer migrationBuffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);

            byte[] corruptLegacyKey = new byte[] { 1, 2, 3 };
            assertNull(invoke(migrator, "migrateKey",
                              new Class[] { byte[].class, StoreFmt.class, StoreFmt.Parser.class, ByteBuffer.class,
                                      StoreFmt.class, StoreFmt.Encoder.class },
                              corruptLegacyKey, sourceFormat, parser, migrationBuffer, targetFormat, encoder));

            byte[] tooLongHashKey = new byte[targetFormat.getHasher().sizeInBytes() * 3 + 1];
            assertNull(invoke(migrator, "migrateKey",
                              new Class[] { byte[].class, StoreFmt.class, StoreFmt.Parser.class, ByteBuffer.class,
                                      StoreFmt.class, StoreFmt.Encoder.class },
                              tooLongHashKey, targetFormat, targetFormat.createParser(),
                              ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN), targetFormat, encoder));

            assertNull(invoke(migrator, "migrateValue",
                              new Class[] { byte[].class, StoreFmt.Parser.class, ByteBuffer.class },
                              corruptLegacyKey, parser, ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN)));

            ByteBuffer labelsBuffer = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN);
            StoreFmt.formatLabels(labelsBuffer, List.of(Label.fromText("a"), Label.fromText("b")));
            byte[] multipleLabels = DictionaryLabelStoreRocksDB.asByteArray(labelsBuffer.flip());

            InvocationTargetException ex = assertThrows(InvocationTargetException.class,
                                                        () -> invoke(migrator, "migrateValue",
                                                                     new Class[] { byte[].class, StoreFmt.Parser.class,
                                                                             ByteBuffer.class },
                                                                     multipleLabels, parser,
                                                                     ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN)));
            assertInstanceOf(IllegalStateException.class, ex.getCause());
            assertTrue(ex.getCause().getMessage().contains("multiple distinct labels"));
        }
    }

    private DictionaryLabelStoreRocksDB createStore(File dir) throws Exception {
        return new DictionaryLabelStoreRocksDB(dir, new StoreFmtByHash(HasherUtil.createXX128Hasher()));
    }

    private static Quad concreteQuad() {
        return Quad.create(Quad.defaultGraphIRI,
                           NodeFactory.createURI("http://example/s"),
                           NodeFactory.createURI("http://example/p"),
                           NodeFactory.createLiteralString("o"));
    }

    private static Object createMigrator(DictionaryLabelStoreRocksDB store) throws Exception {
        Class<?> migratorClass = Arrays.stream(DictionaryLabelStoreRocksDB.class.getDeclaredClasses())
                                       .filter(clazz -> clazz.getSimpleName().equals("LegacyToDictionaryMigrator"))
                                       .findFirst()
                                       .orElseThrow();
        Constructor<?> ctor = migratorClass.getDeclaredConstructor(DictionaryLabelStoreRocksDB.class);
        ctor.setAccessible(true);
        return ctor.newInstance(store);
    }

    private static Object invokeStatic(Class<?> clazz, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = clazz.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args)
            throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }
}
