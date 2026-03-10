package io.telicent.jena.abac.labels.hashing;

/**
 * Wrapper interface for hashers to allow us to be agnostic of the underlying hash function interfaces
 */
public interface Hasher {
    /**
     * Hashes the given input string
     * @param input Input string
     * @return Hash bytes
     */
    byte[] hash(String input);

    /**
     * Hashes the given input bytes
     * @param input Input bytes
     * @return Hash bytes
     */
    byte[] hash(byte[] input);
}