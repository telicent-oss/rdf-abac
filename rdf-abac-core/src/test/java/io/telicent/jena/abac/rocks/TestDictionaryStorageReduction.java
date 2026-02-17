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

package io.telicent.jena.abac.rocks;

import io.telicent.jena.abac.labels.*;
import io.telicent.smart.cache.storage.labels.DictionaryLabelsStore;
import org.apache.commons.io.FileUtils;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Comparative test that measures whether dictionary-based label encoding
 * reduces RocksDB storage compared to plain text encoding.
 * <p>
 * The test populates two stores with identical data — one without a dictionary,
 * one with — compacts both, and compares storage metrics:
 * <ul>
 *   <li><b>approxsize</b> — RocksDB's estimate of data in the main label store
 *       (excludes the dictionary's own RocksDB instance)</li>
 *   <li><b>Main store disk</b> — on-disk size of the main label store directory
 *       (excluding the dictionary subdirectory)</li>
 *   <li><b>Total disk</b> — full on-disk size including the dictionary</li>
 * </ul>
 */
public class TestDictionaryStorageReduction {

    private static final Label SMALL_LABEL = Label.fromText("classification=OS");
    private static final Label MEDIUM_LABEL = Label.fromText("classification=O&(permitted_nationalities=GBR|permitted_nationalities=NOR)&(permitted_organisations=Telicent|permitted_organisations=Telidollar)");
    private static final Label LARGE_LABEL = Label.fromText("classification=OS&(permitted_nationalities=ALB|permitted_nationalities=AUS|permitted_nationalities=BEL|permitted_nationalities=BGR|permitted_nationalities=CAN|permitted_nationalities=HRV|permitted_nationalities=CZE|ermitted_nationalities=DNK|permitted_nationalities=EST|permitted_nationalities=FIN|permitted_nationalities=FRA|permitted_nationalities=DEU|permitted_nationalities=GRC|permitted_nationalities=HUN|permitted_nationalities=ISL|permitted_nationalities=ITA|permitted_nationalities=LVA|permitted_nationalities=LTU|permitted_nationalities=LUX|permitted_nationalities=MNE|permitted_nationalities=NLD|permitted_nationalities=NZL|permitted_nationalities=MKD|permitted_nationalities=NOR|permitted_nationalities=POL|permitted_nationalities=PRT|permitted_nationalities=ROU|permitted_nationalities=SVK|permitted_nationalities=SVN|permitted_nationalities=ESP|permitted_nationalities=SWE|permitted_nationalities=TUR|permitted_nationalities=GBR|permitted_nationalities=USA)&(permitted_organisations=ALB.ALL|permitted_organisations=AUS.ALL|permitted_organisations=BEL.ALL|permitted_organisations=BGR.ALL|permitted_organisations=CAN.ALL|permitted_organisations=HRV.ALL|permitted_organisations=CZE.ALL|permitted_organisations=DNK.ALL|permitted_organisations=EST.ALL|permitted_organisations=FIN.ALL|permitted_organisations=FRA.ALL|permitted_organisations=DEU.ALL|permitted_organisations=GRC.ALL|permitted_organisations=HUN.ALL|permitted_organisations=ISL.ALL|permitted_organisations=ITA.ALL|permitted_organisations=LVA.ALL|permitted_organisations=LTU.ALL|permitted_organisations=LUX.ALL|permitted_organisations=MNE.ALL|permitted_organisations=NLD.ALL|permitted_organisations=NZL.ALL|permitted_organisations=MKD.ALL|permitted_organisations=NOR.ALL|permitted_organisations=POL.ALL|permitted_organisations=PRT.ALL|permitted_organisations=ROU.ALL|permitted_organisations=SVK.ALL|permitted_organisations=SVN.ALL|permitted_organisations=ESP.ALL|permitted_organisations=SWE.ALL|permitted_organisations=TUR.ALL|permitted_organisations=GBR.ALL|permitted_organisations=USA.ALL|permitted_organisations=GBR.MOD)");
    private static final List<Label> MIXED_LABELS = List.of(SMALL_LABEL, MEDIUM_LABEL, LARGE_LABEL);

    @Test
    public void dictionaryEncoding_reduces_storage_with_25000_triples_and_one_small_label() throws Exception {
        final StoreFmt storeFmt = new StoreFmtByString();
        final LabelsStoreRocksDB.LabelMode labelMode = LabelsStoreRocksDB.LabelMode.Overwrite;
        final int tripleCount = 25_000;
        final List<Label> labels = List.of(SMALL_LABEL);
        runTest(storeFmt, labelMode, tripleCount, labels, 5);
    }

    @Test
    public void dictionaryEncoding_reduces_storage_with_50000_triples_and_one_small_label() throws Exception {
        final StoreFmt storeFmt = new StoreFmtByString();
        final LabelsStoreRocksDB.LabelMode labelMode = LabelsStoreRocksDB.LabelMode.Overwrite;
        final int tripleCount = 50_000;
        final List<Label> labels = List.of(SMALL_LABEL);
        runTest(storeFmt, labelMode, tripleCount, labels, 30);
    }

    @Test
    public void dictionaryEncoding_reduces_storage_with_20000_triples_and_one_medium_label() throws Exception {
        final StoreFmt storeFmt = new StoreFmtByString();
        final LabelsStoreRocksDB.LabelMode labelMode = LabelsStoreRocksDB.LabelMode.Overwrite;
        final int tripleCount = 20_000;
        final List<Label> labels = List.of(MEDIUM_LABEL);
        runTest(storeFmt, labelMode, tripleCount, labels, 25);
    }

    @Test
    public void dictionaryEncoding_reduces_storage_with_50000_triples_and_one_medium_label() throws Exception {
        final StoreFmt storeFmt = new StoreFmtByString();
        final LabelsStoreRocksDB.LabelMode labelMode = LabelsStoreRocksDB.LabelMode.Overwrite;
        final int tripleCount = 50_000;
        final List<Label> labels = List.of(MEDIUM_LABEL);
        runTest(storeFmt, labelMode, tripleCount, labels, 55);
    }

    @Test
    public void dictionaryEncoding_reduces_storage_with_1000_triples_and_one_very_large_label() throws Exception {
        final StoreFmt storeFmt = new StoreFmtByString();
        final LabelsStoreRocksDB.LabelMode labelMode = LabelsStoreRocksDB.LabelMode.Overwrite;
        final int tripleCount = 1_000;
        final List<Label> labels = List.of(LARGE_LABEL);
        runTest(storeFmt, labelMode, tripleCount, labels, 10);
    }

    @Test
    public void dictionaryEncoding_reduces_storage_with_50000_triples_and_one_very_large_label() throws Exception {
        final StoreFmt storeFmt = new StoreFmtByString();
        final LabelsStoreRocksDB.LabelMode labelMode = LabelsStoreRocksDB.LabelMode.Overwrite;
        final int tripleCount = 50_000;
        final List<Label> labels = List.of(LARGE_LABEL);
        runTest(storeFmt, labelMode, tripleCount, labels, 95);
    }

    @Test
    public void dictionaryEncoding_reduces_storage_with_10000_triples_and_mixed_labels() throws Exception {
        final StoreFmt storeFmt = new StoreFmtByString();
        final LabelsStoreRocksDB.LabelMode labelMode = LabelsStoreRocksDB.LabelMode.Overwrite;
        final int tripleCount = 10_000;
        runTest(storeFmt, labelMode, tripleCount, MIXED_LABELS, 65);
    }

    @Test
    public void dictionaryEncoding_reduces_storage_with_20000_triples_and_mixed_labels() throws Exception {
        final StoreFmt storeFmt = new StoreFmtByString();
        final LabelsStoreRocksDB.LabelMode labelMode = LabelsStoreRocksDB.LabelMode.Overwrite;
        final int tripleCount = 20_000;
        runTest(storeFmt, labelMode, tripleCount, MIXED_LABELS, 80);
    }

    /**
     * Populate a store with generated triples, cycling through the label set.
     * This simulates a realistic scenario where many triples share a small
     * number of distinct security labels.
     */
    private void populateStore(LabelsStore store, int count, List<Label> labels) {
        for (int i = 0; i < count; i++) {
            Triple triple = Triple.create(
                    NodeFactory.createURI("http://example.org/s/" + i),
                    NodeFactory.createURI("http://example.org/p/" + (i % 10)),
                    NodeFactory.createURI("http://example.org/o/" + i)
            );
            Label label = labels.get(i % labels.size());
            store.add(triple, label);
        }
    }

    private void runTest(final StoreFmt storeFmt, final LabelsStoreRocksDB.LabelMode labelMode, final int tripleCount, final List<Label> labels, final int expectedReduction) throws Exception {
        // --- Store WITHOUT dictionary ---
        final File dbRoot = Files.createTempDirectory("database").toFile();
        try {
            final ResultMetrics regularMetrics = testWithoutDictionary(dbRoot, labelMode, storeFmt, tripleCount, labels);

            // --- Store WITH dictionary ---
            final File dictionaryDir = Files.createTempDirectory("dictionary").toFile();
            try {
                final ResultMetrics dictionaryMetrics = testWithDictionary(dbRoot, labelMode, storeFmt, tripleCount, labels);

                // --- Report ---
                printReport(regularMetrics, dictionaryMetrics, tripleCount, labels);


                // --- Assertions ---
                assertEquals(regularMetrics.entryCount, dictionaryMetrics.entryCount,
                        "Both stores should have the same number of entries");

                // The main store's data (approxsize) should be smaller with dictionary
                // encoding since 8-byte integer IDs replace variable-length label text.
                assertTrue(dictionaryMetrics.approxSize < regularMetrics.approxSize,
                        String.format("Dictionary main store approxsize (%,d bytes) should be smaller than " +
                                "non-dictionary approxsize (%,d bytes)", dictionaryMetrics.approxSize, regularMetrics.approxSize));

                // The main store's on-disk footprint (excluding dictionary) should also
                // be smaller, confirming the encoding change persists through compaction.
                assertTrue(dictionaryMetrics.totalDiskSize < regularMetrics.totalDiskSize,
                        String.format("Dictionary main store disk (%,d bytes) should be smaller than " +
                                "non-dictionary store disk (%,d bytes)", dictionaryMetrics.dictionaryMainStoreDiskSize, regularMetrics.dictionaryDiskSize));

                // At this scale the main store savings should outweigh the fixed
                // dictionary overhead, yielding a net total disk reduction.
                assertTrue(dictionaryMetrics.totalDiskSize < regularMetrics.totalDiskSize,
                        String.format("Dictionary total disk (%,d bytes) should be smaller than " +
                                "non-dictionary store disk (%,d bytes)", dictionaryMetrics.totalDiskSize, regularMetrics.totalDiskSize));
                final double totalReduction = 100.0 * (1.0 - (double) dictionaryMetrics.totalDiskSize / regularMetrics.totalDiskSize);
                assertTrue(totalReduction > expectedReduction);
            } finally {
                FileUtils.deleteDirectory(dictionaryDir);
            }
        } finally {
            FileUtils.deleteDirectory(dbRoot);
        }
    }

    private ResultMetrics testWithoutDictionary(File dbRoot, LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt, int tripleCount, List<Label> labels) throws Exception {
        try (final LabelsStore noDictStore = Labels.createLabelsStoreRocksDB(dbRoot, labelMode, null, storeFmt)) {
            populateStore(noDictStore, tripleCount, labels);
            Labels.compactLabelsStoreRocksDB(noDictStore);
            final Map<String, String> noDictProps = noDictStore.getProperties();
            final ResultMetrics resultMetrics = new ResultMetrics(
                    Long.parseLong(noDictProps.get("approxsize")),
                    Long.parseLong(noDictProps.get("size")),
                    FileUtils.sizeOfDirectory(dbRoot),
                    null, null);
            Labels.closeLabelsStoreRocksDB(noDictStore);
            Labels.rocks.clear();
            return resultMetrics;
        }
    }

    private ResultMetrics testWithDictionary(File dbRoot, LabelsStoreRocksDB.LabelMode labelMode, StoreFmt storeFmt, int tripleCount, List<Label> labels) throws Exception {
        final File dictSubDir = new File(dbRoot, "dictionary");
        dictSubDir.mkdirs();
        try (final DictionaryLabelsStore dictLabelsStore = Labels.createDictionaryLabelsStore(dictSubDir, 1000)) {
            try (final LabelsStore dictStore = Labels.createLabelsStoreRocksDB(dbRoot, labelMode, null, storeFmt, dictLabelsStore)) {
                populateStore(dictStore, tripleCount, labels);
                Labels.compactLabelsStoreRocksDB(dictStore);
                Map<String, String> dictProps = dictStore.getProperties();
                final long totalDiskSize = FileUtils.sizeOfDirectory(dbRoot);
                final long dictionaryDiskSize = FileUtils.sizeOfDirectory(dictSubDir);
                final ResultMetrics resultMetrics = new ResultMetrics(
                        Long.parseLong(dictProps.get("approxsize")),
                        Long.parseLong(dictProps.get("size")),
                        totalDiskSize,
                        dictionaryDiskSize,
                        totalDiskSize - dictionaryDiskSize);
                Labels.closeLabelsStoreRocksDB(dictStore);
                Labels.rocks.clear();
                return resultMetrics;
            }
        }
    }

    private void printReport(final ResultMetrics regularMetrics, final ResultMetrics dictionaryMetrics, final int tripleCount, final List<Label> labels) {
        // --- Report ---
        System.out.printf("%n=== Dictionary Storage Reduction Test ===%n");
        System.out.printf("Triples inserted: %,d%n", tripleCount);
        System.out.printf("Unique labels:    %d%n", labels.size());
        System.out.printf("%n");
        System.out.printf("%-30s %15s %15s%n", "", "No Dictionary", "Dictionary");
        System.out.printf("%-30s %,15d %,15d%n", "Entry count", regularMetrics.entryCount, dictionaryMetrics.entryCount);
        System.out.printf("%-30s %,15d %,15d%n", "Approx size (bytes)", regularMetrics.approxSize, dictionaryMetrics.approxSize);
        System.out.printf("%-30s %,15d %,15d%n", "Main store disk (bytes)", regularMetrics.totalDiskSize, dictionaryMetrics.dictionaryMainStoreDiskSize);
        System.out.printf("%-30s %15s %,15d%n", "Dictionary disk (bytes)", "n/a", dictionaryMetrics.dictionaryDiskSize);
        System.out.printf("%-30s %,15d %,15d%n", "Total disk (bytes)", regularMetrics.totalDiskSize, dictionaryMetrics.totalDiskSize);
        System.out.printf("%n");
        if (regularMetrics.approxSize > 0) {
            final double approxReduction = 100.0 * (1.0 - (double) dictionaryMetrics.approxSize / regularMetrics.approxSize);
            System.out.printf("%-30s %14.1f%%%n", "Approx size reduction", approxReduction);
        }
        if (regularMetrics.totalDiskSize > 0) {
            final double mainStoreReduction = 100.0 * (1.0 - (double) dictionaryMetrics.dictionaryMainStoreDiskSize / regularMetrics.totalDiskSize);
            final double totalReduction = 100.0 * (1.0 - (double) dictionaryMetrics.totalDiskSize / regularMetrics.totalDiskSize);
            System.out.printf("%-30s %14.1f%%%n", "Main store disk reduction", mainStoreReduction);
            System.out.printf("%-30s %14.1f%%%n", "Total disk reduction", totalReduction);
        }
        System.out.printf("%n");
        System.out.printf("%n");
    }

    private record ResultMetrics(
            Long approxSize,
            Long entryCount,
            Long totalDiskSize,
            Long dictionaryDiskSize,
            Long dictionaryMainStoreDiskSize) {
    }

    ;
}