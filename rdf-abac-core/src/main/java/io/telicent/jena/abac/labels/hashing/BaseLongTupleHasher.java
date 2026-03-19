package io.telicent.jena.abac.labels.hashing;

import net.openhft.hashing.LongTupleHashFunction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Base class for Hash Functions implemented to the LZ4 standard.
 * Each function is greater than 64-bit so individual longs cannot be the return value.
 */
public class BaseLongTupleHasher extends NamedHasher {
    final LongTupleHashFunction hashFunction;

    protected BaseLongTupleHasher(LongTupleHashFunction hashFunction, String name) {

        super(name);
        this.hashFunction = hashFunction;
    }

    @Override
    public byte[] hash(String input) {
        long[] hashValue = hashFunction.hashChars(input);
        return longArrayToByteArray(hashValue);
    }

    @Override
    public byte[] hash(byte[] input) {
        long[] hashValue = hashFunction.hashBytes(input);
        return longArrayToByteArray(hashValue);
    }

    public static byte[] longArrayToByteArray(long[] longArray) {
        // Each long is 8 bytes, so the byte array should be longArray.length * 8 in size
        byte[] byteArray = new byte[longArray.length * Long.BYTES];

        // Use a ByteBuffer to convert the long array to bytes
        ByteBuffer byteBuffer = ByteBuffer.wrap(byteArray);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN); // You can set the byte order as needed

        // Put each long value into the ByteBuffer
        for (long value : longArray) {
            byteBuffer.putLong(value);
        }
        return byteArray;
    }

}
