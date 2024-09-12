package io.telicent.jena.abac.labels.hashing;

import net.openhft.hashing.LongTupleHashFunction;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Base class for Hash Functions implemented to the LZ4 standard.
 * Each function is greater than 64-bit so individual longs cannot be the return value.
 */
public class BaseLongTupleHasher implements Hasher {
    final LongTupleHashFunction hashFunction;

    protected BaseLongTupleHasher(LongTupleHashFunction hashFunction) {
        this.hashFunction = hashFunction;
    }

    @Override
    public byte[] hash(String input) {
        long[] hashValue = hashFunction.hashChars(input);
        return longArrayToByteArray(hashValue);
    }

    /**
     * To aid with testing - provide a name
     * @return a toString() of the underlying function
     */
    @Override
    public String toString() {
        String className = hashFunction.getClass().getName();
        int lastDollarIndex = className.lastIndexOf('$');
        if (lastDollarIndex >= 0) {
            className = className.substring(0, lastDollarIndex);
        }
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            className = className.substring(lastDotIndex + 1);
        }
        return className;
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
