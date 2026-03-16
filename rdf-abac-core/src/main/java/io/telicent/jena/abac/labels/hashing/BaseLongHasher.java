package io.telicent.jena.abac.labels.hashing;

import net.openhft.hashing.LongHashFunction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Base class for Hash Functions implemented to the LZ4 standard.
 * Each function is 64-bit due to Long being the return value.
 */
public class BaseLongHasher extends NamedHasher {
    final LongHashFunction hashFunction;

    protected BaseLongHasher(LongHashFunction hashFunction, String name)
    {
        super(name);
        this.hashFunction = hashFunction;
    }

    @Override
    public byte[] hash(String input) {
        long hashValue = hashFunction.hashChars(input);
        return formatLongVariable(hashValue);
    }

    public byte[] hash(byte[] input) {
        long hashValue = hashFunction.hashBytes(input);
        return formatLongVariable(hashValue);
    }

    /**
     * Convert long to byte[] but use less space if we can.
     * @param value to convert
     * @return byte[] of smallest size possible.
     */
    static byte[] formatLongVariable(long value) {
        ByteBuffer byteBuffer;
        if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
            // Value fits in 8 bytes (long)
            byteBuffer = ByteBuffer.allocate(Long.BYTES);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putLong(value);
        } else if (value > Short.MAX_VALUE || value < Short.MIN_VALUE) {
            // Value fits in 4 bytes (int)
            byteBuffer = ByteBuffer.allocate(Integer.BYTES);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putInt((int) value);
        } else if (value > Byte.MAX_VALUE || value < Byte.MIN_VALUE) {
            // Value fits in 2 bytes (short)
            byteBuffer = ByteBuffer.allocate(Short.BYTES);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.putShort((short) value);
        } else {
            // Value fits in 1 byte
            byteBuffer = ByteBuffer.allocate(Byte.BYTES);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
            byteBuffer.put((byte) value);
        }

        return byteBuffer.array();
    }
}