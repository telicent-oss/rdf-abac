package io.telicent.jena.abac.bulk;

import io.telicent.jena.abac.labels.Labels;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import static io.telicent.jena.abac.bulk.BulkDirectory.LOG;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Common base setup extension for RocksDB-based label store tests.
 * <p>
 * Concrete subclasses know how to create different kinds of label stores for testing.
 */
class RocksDBSetupExtension implements BeforeEachCallback, AfterEachCallback {

    private File dbDir;
    private LabelsStore labelsStore;

    private final ExecutorService shellExecutorService = Executors.newSingleThreadExecutor();

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        LOG.info("RocksDB content before compaction");
        logRocksDBContents(dbDir);
        Labels.compactLabelsStoreRocksDB(labelsStore);
        LOG.info("RocksDB content after compaction");
        logRocksDBContents(dbDir);
        Labels.closeLabelsStoreRocksDB(labelsStore);
        FileUtils.deleteDirectory(dbDir);
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        dbDir = Files.createTempDirectory("tmpDirPrefix").toFile();
        LOG.info("RocksDB directory {} for test {}", dbDir, extensionContext.getDisplayName());

        //Set the "dbDir" field in the test itself to be the one we just created
        var rocksDBTests = extensionContext.getTestInstance().get();
        var clz = rocksDBTests.getClass();
        clz.getField("dbDir").set(rocksDBTests, dbDir);
    }

    private void logRocksDBContents(final File dbDir) {
        try {
            AtomicLong MB = new AtomicLong();
            AtomicLong sstFileCount = new AtomicLong();
            Consumer<String> lineFn = (String s) -> {
                if (LOG.isDebugEnabled()) {
                    System.out.println(s);
                }
                var columns = s.split( "\\s+");
                if (columns.length > 8 &&
                    columns[8].endsWith(".sst")) {
                    MB.addAndGet(Long.parseLong(columns[4]) >> 20);
                    sstFileCount.incrementAndGet();
                }
            };
            var process = Runtime.getRuntime().exec(new String[]{
                "ls", "-l", dbDir.toString()
            });
            StreamGobbler streamGobbler =
                new StreamGobbler(process.getInputStream(), lineFn);
            Future<?> future = shellExecutorService.submit(streamGobbler);

            int exitCode = process.waitFor();
            future.get(5, TimeUnit.SECONDS);
            LOG.info("SST file count " + sstFileCount + ", total MB " + MB);
        } catch (IOException | ExecutionException | InterruptedException | TimeoutException e) {
            LOG.error("Could not dump RocksDB info", e);
        }
    }

    private static class StreamGobbler implements Runnable {
        private InputStream inputStream;
        private Consumer<String> consumer;

        public StreamGobbler(InputStream inputStream, Consumer<String> consumer) {
            this.inputStream = inputStream;
            this.consumer = consumer;
        }

        @Override
        public void run() {
            new BufferedReader(new InputStreamReader(inputStream)).lines()
                .forEach(consumer);
        }
    }
}

