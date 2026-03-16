package io.telicent.jena.abac.labels.hashing;

import java.util.Objects;

/**
 * Abstract hasher implementation that stores a name for the hash so Hasher instances have a unique {@link #toString()}
 * value.
 * <p>
 * This is necessary because some hashes within the same family share the same implementation class but use different
 * parameters and/or transforms to yield the hash output.  So just using the implementation class name (as we did prior
 * to {@code 3.0.0}) to identify and check whether the correct hash function has been configured is insufficient.
 * </p>
 */
public abstract class NamedHasher implements Hasher {
    protected final String name;

    /**
     * Creates a new named hasher
     *
     * @param name Hash name
     */
    public NamedHasher(String name) {
        this.name = Objects.requireNonNull(name);
    }

    @Override
    public abstract byte[] hash(String input);

    @Override
    public abstract byte[] hash(byte[] input);

    /**
     * Returns a unique name for the hash function
     *
     * @return Hash function name
     */
    @Override
    public final String toString() {
        return this.name;
    }
}
