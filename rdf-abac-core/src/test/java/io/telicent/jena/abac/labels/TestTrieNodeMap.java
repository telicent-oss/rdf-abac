package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.SysABAC;
import io.telicent.jena.abac.labels.node.table.NaiveNodeTable;
import io.telicent.jena.abac.labels.node.table.TrieNodeTable;
import io.telicent.platform.play.PlayFiles;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.tdb2.store.NodeId;
import org.apache.jena.tdb2.store.nodetable.NodeTable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rocksdb.RocksDBException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

public class TestTrieNodeMap {

    protected static Logger LOG = LoggerFactory.getLogger(TestTrieNodeMap.class);

    @Test
    public void add() {

        TrieNodeTable nodeTable = new TrieNodeTable();
        var ids = new HashSet<NodeId>();
        var id1 = nodeTable.getAllocateNodeId(NodeFactory.createURI("https://www.one.two.three/four#five"));
        var id2 = nodeTable.getAllocateNodeId(NodeFactory.createURI("https://www.one.two.three/four#six"));
        var id3 = nodeTable.getAllocateNodeId(NodeFactory.createURI("https://www.one.two.three/four#five"));
        assertThat(id1).isNotEqualTo(id2);
        ids.add(id1);
        ids.add(id2);
        ids.add(id3);
        assertThat(ids.size()).isEqualTo(2);

        var id4 = nodeTable.getAllocateNodeId(NodeFactory.createURI("https://www.one.three/four#five"));
        ids.add(id4);
        assertThat(ids.size()).isEqualTo(3);

        var id1_2 = nodeTable.getAllocateNodeId(NodeFactory.createURI("https://www.one.two.three/four#five"));
        assertThat(id1_2).isEqualTo(id1);
        var id2_2 = nodeTable.getAllocateNodeId(NodeFactory.createURI("https://www.one.two.three/four#six"));
        assertThat(id2_2).isEqualTo(id2);
        var id3_2 = nodeTable.getAllocateNodeId(NodeFactory.createURI("https://www.one.two.three/four#five"));
        assertThat(id3_2).isEqualTo(id3);
    }

    @Test
    public void sameLeafDifferentTypeDifferentId() {
        TrieNodeTable nodeTable = new TrieNodeTable();
        var id1 = nodeTable.getAllocateNodeId(NodeFactory.createURI("https://www.one.two.three/four#five"));
        var id2 = nodeTable.getAllocateNodeId(NodeFactory.createLiteralString("https://www.one.two.three/four#five"));
        assertThat(id1).isNotEqualTo(id2);
    }

    private final static String RELATIVE_DIR = "src/test/files/starwars";
    private final static String DEFAULT_SECURITY_LABEL = "security=unknowndefault";

    private LabelsStore loadWithNodeTable(final LabelsStoreRocksDB.LabelMode labelMode, final NodeTable nodeTable) throws RocksDBException, IOException {

        var dbDir = Files.createTempDirectory("tmpDirPrefix").toFile();
        var labelsStore = Labels.createLabelsStoreRocksDBById(dbDir, nodeTable, labelMode, null);

        File files = new File(RELATIVE_DIR);
        assertThat(files.isDirectory()).isTrue();
        PlayFiles.action(files.getAbsolutePath(),
            message -> LabelsLoadingConsumer.consume(labelsStore, message),
            headers -> headers.put(SysABAC.hSecurityLabel, DEFAULT_SECURITY_LABEL));
        final var properties = labelsStore.getProperties();
        LOG.info("properties {}", properties);

        return labelsStore;
    }

    /**
     * check they have the same number of entries, and the same ids allocated
     * give ourselves some confidence that the Trie-based implementation is OK
     *
     * @throws IOException if underlying RocksDB fails
     * @throws RocksDBException if underlying RocksDB fails
     */
    @ParameterizedTest
    @EnumSource(LabelsStoreRocksDB.LabelMode.class)
    public void compareNodeTableImplementations(LabelsStoreRocksDB.LabelMode labelMode) throws IOException, RocksDBException {
        var nodeTableNaiveId = new NaiveNodeTable();
        var labelsStoreNaiveId = loadWithNodeTable(labelMode, nodeTableNaiveId);
        var nodeTableTrieId = new TrieNodeTable();
        var labelsStoreTrieId = loadWithNodeTable(labelMode, nodeTableTrieId);

        var countNaive = 0;
        for (var it = nodeTableNaiveId.all(); it.hasNext();) {
            countNaive++;
            var entry = it.next();
            //System.err.println("car: " + entry.car() + ", cdr: " + entry.cdr());
        }
        System.err.println("NaiveNodeTable " + countNaive + " items.");

        var countTrie = 0;
        for (var it = nodeTableTrieId.all(); it.hasNext();) {
            countTrie++;
            var entry = it.next();
            System.err.println("car: " + entry.car() + ", cdr: " + entry.cdr());
        }
        System.err.println("TrieNodeTable " + countTrie + " items.");
        assertThat(countTrie).isEqualTo(countNaive);

        var countChecks = 0;
        for (var it = nodeTableNaiveId.all(); it.hasNext();) {
            var entry = it.next();
            var trieId = nodeTableTrieId.getAllocateNodeId(entry.cdr());
            assertThat(trieId).isEqualTo(entry.car());
            countChecks++;
        }
        assertThat(countChecks).isEqualTo(countNaive);
    }
}
