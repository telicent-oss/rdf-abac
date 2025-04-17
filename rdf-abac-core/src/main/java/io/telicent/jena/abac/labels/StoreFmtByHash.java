package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.labels.hashing.Hasher;
import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 *
 *  An id-based implementation of a storage format for {@code RocksDB}-based label stores.
 *  <p>
 *  Because it is using a hash to generate the ID, it is a one way process and so for processing Labels,
 *  we will use the existing String parsing {@code parseStrings()}.
 */
public class StoreFmtByHash implements StoreFmt {

    private final Hasher hasher;

    public StoreFmtByHash(Hasher hasher) {
        this.hasher = hasher;
    }

    @Override
    public Encoder createEncoder() {
        return new HashEncoder(hasher);
    }

    @Override
    public Parser createParser() {
        return new OnlyStringParser();
    }

    /**
     * To aid with testing - provide a name
     * @return a toString()
     */
    @Override
    public String toString() {
        String className = getClass().toString();
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            className = className.substring(lastDotIndex + 1);
        }
        return className;
    }

    static String encodeNodeAsString(Node node) {
        return switch (NodeType.of(node)) {
            case Any -> "*";
            case URI -> node.getURI();
            case Literal -> node.getLiteral().getLexicalForm();
            case Blank -> node.getBlankNodeLabel();
        };
    }

    public class HashEncoder implements Encoder {
        public final Hasher hasher;

        public HashEncoder(Hasher hasher) {
            this.hasher = hasher;
        }

        @Override
        public Encoder formatSingleNode(ByteBuffer byteBuffer, Node node) {
            String stringRepresentation = encodeNodeAsString(node);
            byte[] byteRepresentation = hashInput(stringRepresentation);
            byteBuffer.put(byteRepresentation);
            return this;
        }

        @Override
        /*
         * This is only used to encode the labels themselves thus we will not hash it.
         * It needs to be a reversible action.
         */
        public Encoder formatLabels(ByteBuffer byteBuffer, List<Label> labels) {
            StoreFmt.formatLabels(byteBuffer, labels);
            return this;
        }

        @Override
        public Encoder formatTriple(ByteBuffer byteBuffer, Node subject, Node predicate, Node object) {
            formatSingleNode(byteBuffer, subject);
            formatSingleNode(byteBuffer, predicate);
            formatSingleNode(byteBuffer, object);

            return this;
        }

        private byte[] hashInput(String input) {
            return hasher.hash(input);
        }
    }

    /**
     * This class will only ever parse Strings - since it's reversible;
     * as the hashing functions used in encoding are otherwise one-way only.
     */
    public static class OnlyStringParser implements Parser {

        private final CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder();

        @Override
        /* NO-OP */
        public Node parseSingleNode(ByteBuffer byteBuffer) {
            throw new NotImplemented();
        }

        @Override
        /* NO-OP */
        public Parser parseTriple(ByteBuffer byteBuffer, List<Node> spo) {
            throw new NotImplemented();
        }

        @Override
        public Parser parseLabels(ByteBuffer valueBuffer, Collection<Label> labels) {
            StoreFmt.parseLabels(valueBuffer, decoder, labels);
            return this;
        }
    }
}