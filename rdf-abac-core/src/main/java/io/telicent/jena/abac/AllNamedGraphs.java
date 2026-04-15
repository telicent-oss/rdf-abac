package io.telicent.jena.abac;

import org.apache.jena.atlas.lib.NotImplemented;
import org.apache.jena.graph.Node;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.Quad;

import java.util.*;

/**
 * A wrapper to allow exposing all named graphs in the underlying dataset by default to a
 * {@link org.apache.jena.sparql.core.DatasetGraphFilteredView}.  Note that this implements only the methods of the
 * interface required for {@link org.apache.jena.sparql.core.DatasetGraphFilteredView} to function, any other method
 * will throw {@link NotImplemented}.  Hence, this class is private to restrict its usage only to RDF-ABAC internal
 * needs.
 * <p>
 * Internally this computes and caches the set of visible named graphs once, and only once, for its lifetime.  This
 * ensures that even if the computation is expensive we only incur its cost once.
 * </p>
 */
class AllNamedGraphs implements Collection<Node> {

    private final DatasetGraph dsg;
    private Set<Node> namedGraphs = null;

    /**
     * Creates a new wrapper
     *
     * @param dsg Underlying dataset whose named graphs we wish to make visible
     */
    public AllNamedGraphs(DatasetGraph dsg) {
        this.dsg = Objects.requireNonNull(dsg, "Dataset Graph cannot be null");
    }

    /**
     * Obtain and cache the list of named graphs for the lifetime of this request once, and only once, since obtaining
     * this from the underlying storage may be an expensive operation.  For TDB2 at least this should be an efficient
     * prefix scan over one of the indexes with G as the first portion of the key but for large databases this still can
     * take some time.
     */
    private void ensureNamedGraphs() {
        if (this.namedGraphs == null) {
            this.namedGraphs = new HashSet<>();
            Iterator<Node> it = this.dsg.listGraphNodes();
            if (it != null) {
                while (it.hasNext()) {
                    Node g = it.next();
                    // Ignore null/default graph
                    // DatasetGraphFilteredView will try to "fix" us if we report these as named graphs
                    if (g == null || Quad.isDefaultGraph(g)) {
                        continue;
                    }
                    this.namedGraphs.add(g);
                }
            }
        }
    }

    @Override
    public int size() {
        ensureNamedGraphs();
        return this.namedGraphs.size();
    }

    @Override
    public boolean isEmpty() {
        ensureNamedGraphs();
        return this.namedGraphs.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        ensureNamedGraphs();
        return this.namedGraphs.contains(o);
    }

    @Override
    public Iterator<Node> iterator() {
        ensureNamedGraphs();
        return this.namedGraphs.iterator();
    }

    @Override
    public Object[] toArray() {
        ensureNamedGraphs();
        return this.namedGraphs.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        ensureNamedGraphs();
        return this.namedGraphs.toArray(a);
    }

    @Override
    public boolean add(Node node) {
        throw new NotImplemented();
    }

    @Override
    public boolean remove(Object o) {
        throw new NotImplemented();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        ensureNamedGraphs();
        return this.namedGraphs.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends Node> c) {
        throw new NotImplemented();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new NotImplemented();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new NotImplemented();
    }

    @Override
    public void clear() {
        throw new NotImplemented();
    }
}
