package io.telicent.jena.abac.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import org.apache.jena.graph.Graph;
import org.apache.jena.http.HttpEnv;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.util.FileUtils;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static io.telicent.jena.abac.core.Attributes.hierarchies;

/**
 * {@code AttributesStoreAuthServer} bridges ABAC with a new auth-server model by:
 * <ul>
 *   <li>Owning a lightweight, in-memory cache mapping an opaque ID token → username,
 *       and username → parsed {@link AttributeValueSet} of user attributes.</li>
 *   <li>Optionally providing hierarchy lookups either from a local RDF file (parsed into a local store)
 *       or from a remote HTTP(S) hierarchy endpoint (via {@link AttributesStoreRemote}).</li>
 * </ul>
 *
 * <p>The cache is intended to be populated by upstream components (e.g., a servlet filter) after
 * calling the auth-server {@code /userinfo} endpoint, so that this store can serve attributes without
 * performing additional network calls.</p>
 *
 * <h3>Hierarchy source selection</h3>
 * <ul>
 *   <li>If {@code lookupHierarchyURI} is {@code null} or blank → no hierarchy support.</li>
 *   <li>If {@code lookupHierarchyURI} is a URI (per {@link FileUtils#isURI(String)}) and is not a
 *       {@code file:} URI → a remote hierarchy store is used.</li>
 *   <li>Otherwise, the value is treated as a local file path and parsed into an in-memory store.</li>
 * </ul>
 *
 * <h3>Caching configuration</h3>
 * The caches use Caffeine with a TTL and maximum size configurable via environment variables or
 * system properties:
 * <ul>
 *   <li>{@code ABAC_USERINFO_CACHE_TTL_SECONDS} (default {@code 60})</li>
 *   <li>{@code ABAC_USERINFO_CACHE_MAX_SIZE}   (default {@code 10000})</li>
 * </ul>
 *
 * @see AttributeValueSet
 * @see Hierarchy
 * @see AttributesStoreRemote
 * @see AttributesStoreLocal
 */
public class AttributesStoreAuthServer implements AttributesStore {

    /**
     * Cache: opaque ID (e.g., access token hash) → username.
     */
    private static final Cache<String, String> idToUserName;

    /**
     * Cache: username → attribute value set parsed from /userinfo.
     */
    private static final Cache<String, AttributeValueSet> userNameToAttributes;

    static {
        long ttl = Long.parseLong(Optional.ofNullable(System.getenv("ABAC_USERINFO_CACHE_TTL_SECONDS"))
                .orElse(System.getProperty("ABAC_USERINFO_CACHE_TTL_SECONDS", "60")));
        long max = Long.parseLong(Optional.ofNullable(System.getenv("ABAC_USERINFO_CACHE_MAX_SIZE"))
                .orElse(System.getProperty("ABAC_USERINFO_CACHE_MAX_SIZE", "10000")));
        idToUserName = Caffeine.newBuilder()
                .maximumSize(max)
                .expireAfterWrite(Duration.ofSeconds(ttl))
                .build();
        userNameToAttributes = Caffeine.newBuilder()
                .maximumSize(max)
                .expireAfterWrite(Duration.ofSeconds(ttl))
                .build();
    }

    /**
     * Optional delegate used for hierarchy resolution (local or remote).
     */
    private final AttributesStore underlyingHierarchyStore;

    /**
     * Create a store with optional hierarchy support, using the default Jena HTTP client
     * for any remote calls.
     *
     * @param lookupHierarchyURI A file path, a non-{@code file:} URI string, or {@code null}/blank for no hierarchy.
     *                           See class Javadoc for source selection rules.
     */
    public AttributesStoreAuthServer(String lookupHierarchyURI) {
        this(lookupHierarchyURI, HttpEnv.getDftHttpClient());
    }

    /**
     * Create a store with optional hierarchy support, using the provided {@link HttpClient}
     * for remote hierarchy calls (when applicable).
     *
     * @param lookupHierarchyURI A file path, a non-{@code file:} URI string, or {@code null}/blank for no hierarchy.
     *                           See class Javadoc for source selection rules.
     * @param httpClient         HTTP client used by {@link AttributesStoreRemote} if a remote hierarchy source is selected.
     */
    public AttributesStoreAuthServer(String lookupHierarchyURI, HttpClient httpClient) {
        if (lookupHierarchyURI == null || lookupHierarchyURI.isEmpty()) {
            // No hierarchy
            underlyingHierarchyStore = null;
        } else if (FileUtils.isURI(lookupHierarchyURI) && !lookupHierarchyURI.startsWith("file:")) {
            underlyingHierarchyStore = new AttributesStoreRemote("", lookupHierarchyURI, httpClient);
        } else {
            AttributesStoreLocal localStore = new AttributesStoreLocal();
            Graph graph = RDFDataMgr.loadGraph(lookupHierarchyURI);
            hierarchies(graph, localStore);
            underlyingHierarchyStore = localStore;
        }
    }

    /**
     * Return the attributes previously cached for a given username.
     * <p>
     * This does not query any remote service; values are returned only if they have been
     * populated via {@link #addUserNameAndAttributes(String, AttributeValueSet)}.
     *
     * @param userName the username key
     * @return the {@link AttributeValueSet} for the user, or {@code null} if not cached
     */
    @Override
    public AttributeValueSet attributes(String userName) {
        return userNameToAttributes.getIfPresent(userName);
    }

    /**
     * Return the configured hierarchy for the given attribute name, if any.
     * <p>
     * Delegates to the configured hierarchy store (local or remote) when present. If no hierarchy
     * store was configured, returns {@code null}.
     *
     * @param attribute the attribute for which a hierarchy is requested
     * @return a {@link Hierarchy} instance or {@code null} if none
     */
    @Override
    public Hierarchy getHierarchy(Attribute attribute) {
        if (underlyingHierarchyStore != null) {
            return underlyingHierarchyStore.getHierarchy(attribute);
        }
        return null;
    }

    /**
     * Return the set of usernames for which attributes are currently cached.
     * <p>
     * Note this is <em>not</em> a complete user directory—only users that have been added to
     * this process using {@link #addUserNameAndAttributes(String, AttributeValueSet)} will appear.
     *
     * @return usernames present in the local attribute cache
     */
    @Override
    public Set<String> users() {
        return userNameToAttributes.asMap().keySet();
    }

    /**
     * Determine whether a non-empty hierarchy is available for the given attribute.
     *
     * @param attribute the attribute to check
     * @return {@code true} if a hierarchy exists and contains one or more values; otherwise {@code false}
     */
    @Override
    public boolean hasHierarchy(Attribute attribute) {
        Hierarchy hierarchy = getHierarchy(attribute);
        return hierarchy != null && !hierarchy.values().isEmpty();
    }

    /**
     * Add or replace the mapping from an opaque ID (e.g., an access token or session ID) to a username.
     * <p>
     * Typically called by a request filter after validating /userinfo, so subsequent requests
     * can recover the username from the same ID without round-tripping to the auth-server.
     *
     * @param id       the opaque id (e.g., token)
     * @param userName the resolved username
     */
    public static void addIdAndUserName(String id, String userName) {
        idToUserName.put(id, userName);
    }

    /**
     * Return the username previously cached for the given opaque ID.
     *
     * @param id the opaque id (e.g., token)
     * @return the username or {@code null} if not cached
     */
    public static String getCachedUsername(String id) {
        return idToUserName.getIfPresent(id);
    }

    /**
     * Add or replace the attribute set for a username, as parsed from /userinfo.
     * <p>
     * This is the main integration point for caching the normalized ABAC attributes that will be used
     * by downstream authorization checks.
     *
     * @param userName          the username key
     * @param attributeValueSet the parsed attribute values to cache
     */
    public static void addUserNameAndAttributes(String userName, AttributeValueSet attributeValueSet) {
        userNameToAttributes.put(userName, attributeValueSet);
    }
}
