package io.telicent.jena.abac.labels.hashing;

import com.google.common.hash.HashFunction;

import java.nio.charset.StandardCharsets;

/**
 * Base class for hash functions implementing to the Google standard.
 */
public class BaseHasher implements Hasher {

    HashFunction hashFunction;

    protected BaseHasher(HashFunction function) {
        hashFunction = function;
    }

    /**
     * Takes a string and returns the byte[] hash
     */
    @Override
    public byte[] hash(String input) {
        return hashFunction.hashString(input, StandardCharsets.UTF_8).asBytes();
    }

    /**
     * To aid with testing - provide a name
     * @return a toString() of the underlying function
     */
    @Override
    public String toString() {
        String className = hashFunction.toString();
        int lastDollarIndex = className.lastIndexOf('(');
        if (lastDollarIndex >= 0) {
            className = className.substring(0, lastDollarIndex);
        }
        int lastDotIndex = className.lastIndexOf('.');
        if (lastDotIndex >= 0) {
            className = className.substring(lastDotIndex + 1);
        }
        return className;
    }
}