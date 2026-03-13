package io.telicent.jena.abac.labels.hashing;

import java.util.Objects;

/**
 * Abstract hasher implementation that stores a name for the hash so Hasher instances have a unique {@link #toString()}
 * value.  This is necessary because some hashes within the same family share the same implementation class and trying
 * to just use the hash name to compare whether the correct hash function has been configured is insufficient.
 */
public abstract class AbstractHasher implements Hasher {
    protected final String name;

    /**
     * Creates a new named hasher
     *
     * @param name Hash name
     */
    public AbstractHasher(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public abstract byte[] hash(String input);

    @Override
    public abstract byte[] hash(byte[] input);

    /**
     * Provides a unique name for the hash function
     *
     * @return Hash function name
     */
    @Override
    public final String toString() {
        return this.name;
    }
}
