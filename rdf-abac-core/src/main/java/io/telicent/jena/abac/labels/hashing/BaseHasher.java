package io.telicent.jena.abac.labels.hashing;

import com.google.common.hash.HashFunction;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Base class for hash functions implementing to the Google standard.
 */
public class BaseHasher extends NamedHasher {

    protected HashFunction hashFunction;

    protected BaseHasher(HashFunction function, String name) {
        super(name);
        this.hashFunction = Objects.requireNonNull(function);
    }

    /**
     * Takes a string and returns the byte[] hash
     * @param input Input string
     * @return Hash bytes
     */
    @Override
    public byte[] hash(String input) {
        return hashFunction.hashString(input, StandardCharsets.UTF_8).asBytes();
    }

    /**
     * Takes input byte[] and returns the byte[] hash
     * @param input Input bytes
     * @return Hash bytes
     */
    @Override
    public byte[] hash(byte[] input) { return hashFunction.hashBytes(input).asBytes(); }

}