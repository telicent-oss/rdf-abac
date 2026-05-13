package io.telicent.jena.abac.labels.hashing;

/**
 * Wrapper interface for hashers to allow us to be agnostic of the underlying hash function interfaces
 */
public interface Hasher {
    /**
     * Hashes the given input string
     *
     * @param input Input string
     * @return Hash bytes
     */
    byte[] hash(String input);

    /**
     * Hashes the given input bytes
     *
     * @param input Input bytes
     * @return Hash bytes
     */
    byte[] hash(byte[] input);

    /**
     * Gets the size of the hash in bytes
     * <p>
     * This may be an upper limit if the hash function may compress the resulting hash by using fewer bytes where
     * possible.
     * </p>
     *
     * @return Size of the hash in bytes
     */
    int sizeInBytes();
}