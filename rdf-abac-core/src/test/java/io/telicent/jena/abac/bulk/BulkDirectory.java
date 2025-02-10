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
package io.telicent.jena.abac.bulk;

import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.labels.*;
import io.telicent.platform.play.PlayFiles;
import org.apache.jena.atlas.logging.LogCtl;
import org.apache.jena.graph.Triple;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static io.telicent.jena.abac.labels.LabelsStoreRocksDB.LabelMode.Overwrite;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract base for tests which load large data sets into label stores
 */
public abstract class BulkDirectory {

    protected static Logger LOG = LoggerFactory.getLogger(BulkDirectory.class);

    protected final static String CONTENT_DIR = "src/test/files/starwars/content";
    protected final static String AFTER_DIR = "src/test/files/starwars/after";
    protected final static String DEFAULT_SECURITY_LABEL = "security=unknowndefault";

    public File dbDir;

    private static String level = null;

    @BeforeAll
    public static void beforeClass() {
        level = LogCtl.getLevel(BulkDirectory.LOG);
        LogCtl.setLevel(BulkDirectory.LOG, "warn");
        LogCtl.setLevel(Labels.class, "warn");
    }

    @AfterEach
    public void after() {
        Labels.rocks.clear();
    }

    @AfterAll
    public static void afterClass() {
        if (level != null)
            LogCtl.setLevel(BulkDirectory.LOG, "level");
    }

    abstract LabelsStore createLabelsStore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws RocksDBException;

    static Stream<Arguments> provideLabelAndStorageFmt() {
        return Stream.of(Arguments.of(null, null));
    }

    @Disabled("useful for dumping comprehensive java state")
    @Test
    public void javaInfo() {
        // Also, add -XX:+PrintFlagsFinal to the run configuration to dump HeapSize etc..
        System.err.println("---Properties---");
        System.getProperties().forEach((k, v) -> System.err.println(k + " --> " + v));
        System.err.println("---Env---");
        System.getenv().forEach((k, v) -> System.err.println(k + " --> " + v));
        System.err.println("---Management---");
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        var arguments = runtimeMxBean.getInputArguments();
        arguments.forEach(System.err::println);
        System.err.println("---Runtime---");
        var rtValues = new HashMap<String, Long>();
        var runtime = Runtime.getRuntime();
        rtValues.put("freeMemory", runtime.freeMemory());
        rtValues.put("totalMemory", runtime.totalMemory());
        rtValues.put("maxMemory", runtime.maxMemory());
        var MB = 1 << 20;
        rtValues.forEach((k, v) -> System.err.println(k + " --> " + v / MB + "MB"));
    }

    protected File directoryProperty(final String property) {

        var externalDir = System.getProperty(property);
        assertThat(externalDir).as("Please define property -D%s=<path-to-directory> in your run configuration", property).isNotEmpty();
        File files = new File(externalDir);
        assertThat(files.isDirectory()).isTrue();

        return files;
    }

    @Disabled("too big/slow - used for manually checking load capacity")
    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFmt")
    public void biggerFiles(StoreFmt storeFmt) throws Exception {

        try(LabelsStore labelsStore = createLabelsStore(Overwrite, storeFmt)) {

            File files = directoryProperty("abac.labelstore.biggerfiles");
            PlayFiles.action(files.getAbsolutePath(),
                    message -> LabelsLoadingConsumer.consume(labelsStore, message),
                    headers -> headers.put(SysABAC.hSecurityLabel, DEFAULT_SECURITY_LABEL));

            final var properties = labelsStore.getProperties();
            LOG.info("properties {}", properties);
        }
    }

    @Disabled("too big/slow - used for manually checking load capacity")
    @ParameterizedTest(name = "{index}: Store = {0}")
    @MethodSource("provideStorageFmt")
    public void biggestFiles(StoreFmt storeFmt) throws Exception {

        try(LabelsStore labelsStore = createLabelsStore(Overwrite, storeFmt)) {

            File files = directoryProperty("abac.labelstore.biggestfiles");
            PlayFiles.action(files.getAbsolutePath(),
                    message -> LabelsLoadingConsumer.consume(labelsStore, message),
                    headers -> headers.put(SysABAC.hSecurityLabel, DEFAULT_SECURITY_LABEL));

            final var properties = labelsStore.getProperties();
            LOG.info("properties {}", properties);
        }
    }

    private void playFiles(final String directory, final LabelsStore labelsStore) {
        playFiles(directory, labelsStore, null);
    }

    private void playFiles(final String directory, final LabelsStore labelsStore, final LabelsLoadingConsumer.LabelHandler labelHandler) {
        File files = new File(directory);
        assertThat(files.isDirectory()).isTrue();
        PlayFiles.action(files.getAbsolutePath(),
                message -> LabelsLoadingConsumer.consume(labelsStore, message, labelHandler),
                headers -> headers.put(SysABAC.hSecurityLabel, DEFAULT_SECURITY_LABEL));
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void starWars(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {

        try(LabelsStore labelsStore = createLabelsStore(labelMode, storeFmt)) {

            playFiles(CONTENT_DIR, labelsStore);
            playFiles(AFTER_DIR, labelsStore);

            var labels = LabelsLoadingConsumer.labelsForTriple(labelsStore,
                    "<https://starwars.com#grid_R7> <http://ies.data.gov.uk/ontology/ies4#inLocation> <https://starwars.com#AGalaxyFarFarAway> .");

            if (labelsStore instanceof LabelsStoreRocksDB && labelMode == LabelsStoreRocksDB.LabelMode.Merge) {
                //Check that a member of the AFTER_DIR has BOTH labels
                assertThat(labels.size()).isEqualTo(2);
                assertThat(labels).contains("sensitivity=Ultra");
                assertThat(labels).contains("nationality=GBR");
            } else {
                //Check that a member of the AFTER_DIR has ONLY the latest label (overwrite mode)
                assertThat(labels.size()).isEqualTo(1);
                assertThat(labels).contains("sensitivity=Ultra");
            }

            final var properties = labelsStore.getProperties();
//        LOG.info("properties {}", properties);
            expectedStarWarsProperties().forEach((k, v) -> {
                assertThat(properties.containsKey(k)).isTrue();
                assertThat(properties.get(k)).isEqualTo(v);
            });
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void backupAndRestore(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        // set up
        try(LabelsStore labelsStore = createLabelsStore(labelMode, storeFmt)) {
            String tripleString = "<https://starwars.com#grid_R7> <http://ies.data.gov.uk/ontology/ies4#inLocation> <https://starwars.com#AGalaxyFarFarAway> .";
            String labelBefore = "security=unknowndefault";

            if (labelsStore instanceof LabelsStoreRocksDB rocksDB) {
                Path tempDir = Files.createTempDirectory("backup");
                LabelsLoadingConsumer.addLabelsForTriple(labelsStore, tripleString, labelBefore);
                var labels = LabelsLoadingConsumer.labelsForTriple(labelsStore, tripleString);

                assertEquals(1, labels.size());
                assertEquals(labelBefore, labels.get(0));

                // Back-up
                rocksDB.backup(tempDir.toString());

                // Make some changes
                LabelsLoadingConsumer.addLabelsForTriple(labelsStore, tripleString, "security=somethingelse");
                labels = LabelsLoadingConsumer.labelsForTriple(labelsStore, tripleString);

                if (Overwrite.equals(labelMode)) {
                    assertEquals(1, labels.size());
                    assertNotEquals(labelBefore, labels.get(0));
                } else {
                    assertNotEquals(1, labels.size());
                }

                // Restore
                rocksDB.restore(tempDir.toString());

                labels = LabelsLoadingConsumer.labelsForTriple(labelsStore, tripleString);
                assertEquals(1, labels.size());
                assertEquals(labelBefore, labels.get(0));

                rocksDB.close();

            }
        }
    }

    @ParameterizedTest(name = "{index}: Store = {1}, LabelMode = {0}")
    @MethodSource("provideLabelAndStorageFmt")
    public void backupAndRestoreFailuresThrowLabelsException(LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt) throws Exception {
        try(LabelsStore labelsStore = createLabelsStore(Overwrite, storeFmt)) {

            if (labelsStore instanceof LabelsStoreRocksDB rocksDB) {
                Path tempDir = Files.createTempDirectory("backup");

                String missingDir = tempDir.toString() + "/notthere";

                assertThrows(LabelsException.class,
                        () -> rocksDB.backup(missingDir)
                );

                assertThrows(LabelsException.class,
                        () -> rocksDB.restore(missingDir)
                );
            }
        }
    }

    protected static class LoadStats {
        long beginSetup;
        long beginLoad;
        long beginRead;
        long finish;

        void report(final String message) {
            var setup = Duration.of(beginLoad - beginSetup, ChronoUnit.MILLIS);
            var load = Duration.of(beginRead - beginLoad, ChronoUnit.MILLIS);
            var read = Duration.of(finish - beginRead, ChronoUnit.MILLIS);
            LOG.info("{} : setup {}, load {}, read {}", message, setup, load, read);
        }
    }

    protected LoadStats bulkLoadAndRepeatedlyRead(
            final String filesDir,
            final StoreFmt storeFmt,
            final double readFraction,
            final int readRepeat) throws Exception {

        final LoadStats loadStats = new LoadStats();

        Map<Triple, List<String>> known = new HashMap<>();
        var generator = new Random(42L);

        loadStats.beginSetup = System.currentTimeMillis();
        try(LabelsStore labelsStore = createLabelsStore(Overwrite, storeFmt)){

            loadStats.beginLoad = System.currentTimeMillis();
            AtomicInteger count = new AtomicInteger();
            AtomicInteger overrideCount = new AtomicInteger();
            playFiles(filesDir, labelsStore, (s, p, o, label) -> {
                count.getAndIncrement();
                var random = generator.nextDouble();
                var triple = Triple.create(s, p, o);
                if (random < readFraction) {
                    //change the label, and record the changed label
                    overrideCount.getAndIncrement();
                    var newLabel = String.format("%s_%d", label, overrideCount.get());
                    known.put(triple, List.of(newLabel));
                    labelsStore.add(s, p, o, newLabel);
                    LOG.debug("Add {}", newLabel);
                } else if (random < readFraction + readFraction) {
                    //don't change the label, record the original value to check for
                    known.put(triple, List.of(label));
                } else {
                    if (known.containsKey(triple)) {
                        //handle a repeat by re-overwriting with the value we hold
                        var newLabel = known.get(triple);
                        labelsStore.add(s, p, o, newLabel);
                    }
                }
            });
            LOG.info("{}/{} of database labels overridden for test", known.size(), count);

            var executorService = Executors.newFixedThreadPool(KNOWNLIST_READ_THREADS);

            loadStats.beginRead = System.currentTimeMillis();
            var knownList = new ArrayList<>(known.entrySet());
            for (int i = 0; i < readRepeat; i++) {
                var random = new Random(i);
                Collections.shuffle(knownList, random);
                if (i % 10 == 0) {
                    LOG.debug("Shuffled read repeat {} of {}", i, readRepeat);
                }

                executeKnownList(executorService, labelsStore, knownList);
            }

            loadStats.finish = System.currentTimeMillis();

            return loadStats;
        }
    }

    final static int KNOWNLIST_READ_THREADS = 4;

    private void executeKnownList(
            ExecutorService executorService,
            LabelsStore labelsStore,
            final ArrayList<Map.Entry<Triple, List<String>>> knownList) {
        var futures = new ArrayList<Future<Boolean>>(knownList.size());
        for (var kv : knownList) {
            LOG.debug("Try {} -> {}", kv.getKey(), kv.getValue());
            var future = executorService.submit(() -> {
                var labels = labelsStore.labelsForTriples(kv.getKey());
                assertThat(labels).isEqualTo(kv.getValue());
                return true;
            });
            futures.add(future);
        }
        while (!futures.isEmpty()) {
            try {
                assertThat(futures.remove(0).get()).isEqualTo(true);
            } catch (ExecutionException | InterruptedException e) {
                throw new RuntimeException("Test problem", e);
            }
        }
    }

    protected Map<String, String> expectedStarWarsProperties() {
        return Map.of("size", "20831");
    }

}
