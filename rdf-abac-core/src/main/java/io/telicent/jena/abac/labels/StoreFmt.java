package io.telicent.jena.abac.labels;

import io.telicent.jena.abac.labels.store.rocksdb.legacy.LegacyLabelsStoreRocksDB;
import org.apache.jena.graph.Node;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * The storage format interface for {@code RocksDB}-based label stores. There are storage format implementations for
 * string-based and id-based encoding of nodes into buffers, which are passed to RocksDB for storage.
 * <p>
 * This class also provides a number of static utility methods for parts of the encoding and decoding process which do
 * not vary by (string-based versus id-based) storage formats.
 */
public interface StoreFmt {

    /**
     * The ordering of {@code NodeType} enum values was initially defined for {@code Any} to be last in order to respect
     * how wildcards are processed in seeking {@link LegacyLabelsStoreRocksDB}. As the ordinal values will be encoded on
     * storage, these <strong>MUST NOT BE CHANGED</strong>.
     */
    enum NodeType {
        URI, Literal, Blank, Any;

        static NodeType of(Node node) {
            if (node.isURI()) {
                return NodeType.URI;
            } else if (node.isLiteral()) {
                return NodeType.Literal;
            } else if (node.isBlank()) {
                return NodeType.Blank;
            } else {
                return NodeType.Any;
            }
        }
    }

    ;

    /**
     * Create an encoder that uses this storage format
     *
     * @return a new encoder
     */
    Encoder createEncoder();

    /**
     * Create a parser that uses this storage format
     *
     * @return a new parser
     */
    Parser createParser();

    /**
     * The object that transforms quads, triples, nodes and values into sequences of bytes for storage in
     * {@code RocksDB}
     */
    interface Encoder {

        Encoder formatSingleNode(final ByteBuffer byteBuffer, final Node node);

        /**
         * Formats one/more labels into bytes for storage
         *
         * @param byteBuffer Byte buffer to write into
         * @param strings    Labels
         * @return Self for chaining
         * @deprecated Use {@link #formatLabel(ByteBuffer, Label)} instead as support for storing multiple labels has
         * been deprecated
         */
        @Deprecated
        Encoder formatLabels(final ByteBuffer byteBuffer, final List<Label> strings);

        /**
         * Formats a label into bytes for storage
         *
         * @param byteBuffer Byte buffer to write into
         * @param label      Label
         * @return Self for chaining
         */
        Encoder formatLabel(final ByteBuffer byteBuffer, final Label label);

        /**
         * Formats a triple into bytes for storage
         *
         * @param byteBuffer Byte buffer to write into
         * @param subject    Subject
         * @param predicate  Predicate
         * @param object     Object
         * @return Self for chaining
         * @deprecated Use {@link #formatQuad(ByteBuffer, Node, Node, Node, Node)} instead as labels are now associated
         * with quads rather than triples
         */
        @Deprecated
        Encoder formatTriple(final ByteBuffer byteBuffer, final Node subject, final Node predicate, final Node object);

        /**
         * Formats a quad into bytes for storage
         *
         * @param byteBuffer Byte buffer to write into
         * @param graph      Graph
         * @param subject    Subject
         * @param predicate  Predicate
         * @param object     Object
         * @return Self for chaining
         */
        Encoder formatQuad(final ByteBuffer byteBuffer, final Node graph, final Node subject, final Node predicate,
                           final Node object);

    }

    ;

    /**
     * The object that reads sequences of bytes from storage in {@code RocksDB} back into triples, nodes and values.
     */
    interface Parser {

        /**
         * Parses a single node from the given buffer
         *
         * @param byteBuffer Byte buffer to read from
         * @return Single node
         */
        Node parseSingleNode(final ByteBuffer byteBuffer);

        /**
         * Parses a triple from the given buffer
         *
         * @param byteBuffer Byte buffer to read from
         * @param spo        List to populate with parsed nodes that form the triple
         * @return Self for chaining
         * @deprecated Use {@link #parseQuad(ByteBuffer, List)} as labels are now associated with quads rather than
         * triples
         */
        @Deprecated
        Parser parseTriple(final ByteBuffer byteBuffer, final List<Node> spo);

        /**
         * Parses a quad from the given buffer
         *
         * @param byteBuffer Byte buffer to read from
         * @param gspo       List to populate with parsed nodes that form the quad
         * @return Self for chaining
         */
        Parser parseQuad(final ByteBuffer byteBuffer, final List<Node> gspo);

        /**
         * Parses multiple labels from the given buffer
         *
         * @param valueBuffer Byte buffer to read from
         * @param labels      Labels collection to populate
         * @return Self for chaining
         * @deprecated Use {@link #parseLabel(ByteBuffer)} as support for storing multiple labels has been deprecated
         */
        @Deprecated
        Parser parseLabels(final ByteBuffer valueBuffer, final Collection<Label> labels);

        /**
         * Parses a single label from the given buffer
         *
         * @param valueBuffer Byte buffer to read from
         * @return Parsed label
         */
        Label parseLabel(final ByteBuffer valueBuffer);
    }

    static void encodeNode(Writer writer, Node node) {

        try {
            switch (NodeType.of(node)) {
                case Any:
                    throw new LabelsException("Storing wildcards is no longer supported");
                case URI:
                    writer.write(node.getURI());
                    break;
                case Literal:
                    writer.write(node.getLiteral().getLexicalForm());
                    break;
                case Blank:
                    writer.write(node.getBlankNodeLabel());
                    break;
            }
            ;
        } catch (IOException e) {
            throw new RuntimeException("Exception when formatting/writing node", e);
        }
    }

    /**
     * Encode an integer in the standard number of bytes for an integer (4)
     *
     * @param byteBuffer Buffer to write into
     * @param i          the integer
     */
    static void encodeInt(ByteBuffer byteBuffer, int i) {

        assert byteBuffer.order() == ByteOrder.LITTLE_ENDIAN;
        byteBuffer.putInt(i);
    }

    /**
     * Format a number of strings, preceded by a count of the strings, then an array of the sizes of the strings
     *
     * @param byteBuffer target of the encoding
     * @param labels     to encode
     */
    static void formatLabels(final ByteBuffer byteBuffer, final List<Label> labels) {
        var count = labels.size();
        StoreFmt.encodeInt(byteBuffer, count);
        int[] encodedSizes = new int[count];
        var nextIndex = 0;
        var sizesPosition = byteBuffer.position();
        // Allow space for the string lengths
        var nextPosition = sizesPosition + 4 * count;
        byteBuffer.position(nextPosition);

        var charsetEncoder = StandardCharsets.UTF_8.newEncoder();
        for (Label label : labels) {
            var result = charsetEncoder.encode(CharBuffer.wrap(label.getText()), byteBuffer, true);
            if (!result.isUnderflow()) {
                throw new RuntimeException(
                        new IOException(
                                "Implementation error - format strings - LabelsRocksFormatter's ByteBuffer should be larger than current limit " + byteBuffer.capacity()));
            }
            encodedSizes[nextIndex++] = byteBuffer.position() - nextPosition;
            nextPosition = byteBuffer.position();
        }

        byteBuffer.position(sizesPosition);
        for (int encodedSize : encodedSizes) {
            StoreFmt.encodeInt(byteBuffer, encodedSize);
        }
        byteBuffer.position(nextPosition);
    }

    /**
     * Parse a single string of known number of bytes encoded in a bytebuffer
     *
     * @param byteBuffer containing the encoded string
     * @param decoder    the decoder to use
     * @param len        the number of bytes to decode
     * @return String
     */
    static String parseString(final ByteBuffer byteBuffer, final CharsetDecoder decoder, final int len) {
        var pos = byteBuffer.position();
        var slice = byteBuffer.slice(pos, len);
        var chars = CharBuffer.allocate(len);
        decoder.decode(slice, chars, true);
        byteBuffer.position(pos + len);
        return chars.flip().toString();
    }

    /**
     * Parse back a number of strings encoded as (count,lengths[],strings)
     *
     * @param byteBuffer containing the encoded strings
     * @param decoder    to use to decode to characters
     * @param labels     result list to add decoded strings to
     */
    static void parseLabels(final ByteBuffer byteBuffer, final CharsetDecoder decoder, final Collection<Label> labels) {
        var count = StoreFmt.decodeInt(byteBuffer);
        int[] encodedSizes = new int[count];
        for (int i = 0; i < count; i++) {
            encodedSizes[i] = StoreFmt.decodeInt(byteBuffer);
        }

        for (int i = 0; i < count; i++) {
            int len = encodedSizes[i];
            labels.add(Label.fromText(StoreFmt.parseString(byteBuffer, decoder, len)));
        }
    }


    /**
     * Decode a single 4-byte integer encoded as little endian
     *
     * @param byteBuffer containing the encoded integer
     * @return the integer value retrieved from the bytebuffer
     */
    static int decodeInt(ByteBuffer byteBuffer) {
        assert byteBuffer.order() == ByteOrder.LITTLE_ENDIAN;
        return byteBuffer.getInt();
    }

    enum IntBytes {
        OneByte, TwoBytes, FourBytes, EightBytes;
    }

    /**
     * Format a long value flexibly according to how big it is, as how big it is will be recorded elsewhere (in another
     * part of the encoding, or implicitly).
     *
     * @param byteBuffer to format into
     * @param value      the long to format
     * @return how much space the long value takes up.
     */
    static IntBytes formatLongVariable(final ByteBuffer byteBuffer, long value) {

        assert byteBuffer.order() == ByteOrder.LITTLE_ENDIAN;
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            byteBuffer.putLong(value);
            return IntBytes.EightBytes;
        } else if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            byteBuffer.putInt((int) value);
            return IntBytes.FourBytes;
        } else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            byteBuffer.putShort((short) value);
            return IntBytes.TwoBytes;
        } else {
            byteBuffer.put((byte) value);
            return IntBytes.OneByte;
        }
    }

    /**
     * parse back into a long, which may have been encoded a byte, short, int or long
     *
     * @param byteBuffer contains the encoded long
     * @param size       tells us what the encoding was
     * @return the decoding of the encoded long
     */
    static long parseLongVariable(final ByteBuffer byteBuffer, IntBytes size) {

        assert byteBuffer.order() == ByteOrder.LITTLE_ENDIAN;
        return switch (size) {
            case OneByte -> byteBuffer.get();
            case TwoBytes -> byteBuffer.getShort();
            case FourBytes -> byteBuffer.getInt();
            case EightBytes -> byteBuffer.getLong();
        };
    }

    /**
     * An implementation of {@link java.io.Writer} which writes to a {@link ByteBuffer}
     */
    class Writer extends java.io.Writer {

        Writer(final ByteBuffer byteBuffer, final CharsetEncoder charsetEncoder) {
            this.byteBuffer = byteBuffer;
            this.charsetEncoder = charsetEncoder;
        }

        private final ByteBuffer byteBuffer;
        private final CharsetEncoder charsetEncoder;

        @Override
        public void write(char[] cbuf, int off, int len) throws IOException {
            var charBuffer = CharBuffer.wrap(cbuf, off, len);
            while (true) {
                var pos = byteBuffer.position();
                var result = charsetEncoder.encode(charBuffer, byteBuffer, true);
                if (result.isOverflow()) {
                    // ignore the partial, overflowed encoding, try again in a bigger buffer
                    byteBuffer.position(pos);
                    extendByteBuffer();
                    continue;
                }
                if (!result.isUnderflow()) {
                    throw new IOException(
                            "Implementation error - format labels/triples - LabelsRocksFormatter's ByteBuffer should be larger than current limit " + byteBuffer.capacity());
                }
            }
        }

        @Override
        public void write(String s) throws IOException {
            var charBuffer = CharBuffer.wrap(s);
            while (true) {
                var pos = byteBuffer.position();
                var result = charsetEncoder.encode(charBuffer, byteBuffer, true);
                if (result.isOverflow()) {
                    // ignore the partial, overflowed encoding, try again in a bigger buffer
                    byteBuffer.position(pos);
                    extendByteBuffer();
                    continue;
                }
                if (!result.isUnderflow()) {
                    throw new IOException(
                            "Implementation error - format labels/triples - LabelsRocksFormatter's ByteBuffer should be larger than current limit " + byteBuffer.capacity());
                }
                break;
            }
        }

        private void extendByteBuffer() {
            throw new RuntimeException("extendByteBuffer() not implemented");
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
