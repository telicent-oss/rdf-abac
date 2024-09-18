package io.telicent.jena.abac.labels.hashing;

public interface Hasher {
    byte[] hash(String input);
    String toString();
}