package io.telicent.jena.abac.fuseki.server;

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.core.AttributesStoreAuthServer;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.jena.atlas.json.JSON;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.atlas.json.JsonValue;
import org.apache.jena.fuseki.Fuseki;
import org.apache.jena.fuseki.main.auth.AuthBearerFilter;
import org.apache.jena.riot.web.HttpNames;
import org.slf4j.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * Servlet {@link Filter} that enriches incoming requests with ABAC user context fetched
 * from an auth-server {@code /userinfo} endpoint.
 *
 * <p>When a request carries an {@code Authorization: Bearer <token>} header:
 * <ol>
 *   <li>It first checks the in-process cache managed by
 *       {@link AttributesStoreAuthServer#getCachedUsername(String)}.</li>
 *   <li>If absent, it calls the configured {@code /userinfo} endpoint and flattens the JSON payload
 *       into an {@link AttributeValueSet}, then caches both the username and attributes in
 *       {@link AttributesStoreAuthServer} for subsequent requests.</li>
 *   <li>If the userinfo call fails and a {@link #legacyFilter} is present, the request falls back
 *       to the legacy filter (e.g., header-based user extraction), otherwise the chain continues.</li>
 * </ol>
 *
 * <p>The filter adds the resolved username to the request under
 * {@link #ATTR_ABAC_USERNAME} so downstream components can identify the principal without
 * re-contacting the auth-server.</p>
 *
 * <h3>Configuration</h3>
 * <ul>
 *   <li>{@code ABAC_USERINFO_URL} (env var or system property) — {@code /userinfo} endpoint URL.
 *       Default: {@code http://auth.telicent.localhost:9000/userinfo}.</li>
 * </ul>
 */
public class UserInfoEnrichmentFilter implements Filter {

    /**
     * Request attribute key under which this filter stores the resolved username.
     */
    public static final String ATTR_ABAC_USERNAME = "abac.user";

    /**
     * HTTP client used to call the /userinfo endpoint.
     */
    private final HttpClient http = HttpClient.newHttpClient();

    /**
     * Resolved URL of the /userinfo endpoint.
     */
    private final String userInfoUrl;

    /**
     * Optional legacy bearer filter to invoke if userinfo retrieval fails
     * (e.g., to support "legacy" header-based user extraction).
     */
    private final AuthBearerFilter legacyFilter;

    /**
     * For logging purposes
     */
    private static Logger LOG = Fuseki.configLog;

    /**
     * Construct a filter without a legacy fallback. If the userinfo call fails,
     * the request proceeds in the chain without enrichment.
     */
    public UserInfoEnrichmentFilter() {
        this(null);
    }

    /**
     * Construct a filter with an optional legacy fallback.
     *
     * @param legacyFilter an {@link AuthBearerFilter} to delegate to when userinfo retrieval fails,
     *                     or {@code null} to skip fallback.
     */
    public UserInfoEnrichmentFilter(AuthBearerFilter legacyFilter) {
        this.legacyFilter = legacyFilter;
        this.userInfoUrl = Optional.ofNullable(System.getenv("ABAC_USERINFO_URL"))
                .orElse(System.getProperty("ABAC_USERINFO_URL", "http://auth.telicent.localhost:9000/userinfo"));
    }

    /**
     * Intercepts requests and attempts to enrich them with ABAC user context from {@code /userinfo}.
     *
     * <p>Flow:</p>
     * <ul>
     *   <li>Read {@code Authorization} header; if a Bearer token is present, try caches first.</li>
     *   <li>If not cached, call {@code /userinfo}, flatten attributes, and cache username/attributes.</li>
     *   <li>On failure and if a legacy filter is provided, delegate to it and return.</li>
     *   <li>Continue the chain.</li>
     * </ul>
     *
     * @param req   the request
     * @param resp  the response
     * @param chain the filter chain
     * @throws IOException      if the chain throws
     * @throws ServletException if the chain throws
     */
    @Override
    public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) req;
        String authz = httpReq.getHeader(HttpNames.hAuthorization);

        if (authz != null && authz.toLowerCase(Locale.ROOT).startsWith("bearer ")) {
            String token = authz.substring(7).trim();
            if (!token.isBlank()) {
                String username = AttributesStoreAuthServer.getCachedUsername(token);
                if (username != null) {
                    req.setAttribute(ATTR_ABAC_USERNAME, username);
                } else {
                    CachedUser cu = fetchUserInfo(token);
                    if (cu != null && cu.attributes != null && cu.username != null) {
                        req.setAttribute(ATTR_ABAC_USERNAME, cu.username);
                        // Populate cross-request caches for downstream usage.
                        AttributesStoreAuthServer.addIdAndUserName(token, cu.username);
                        AttributesStoreAuthServer.addUserNameAndAttributes(cu.username, cu.attributes);
                    }
                    if (cu == null && legacyFilter != null) {
                        this.legacyFilter.doFilter(req, resp, chain);
                        return;
                    }
                }
            }
        }
        chain.doFilter(req, resp);
    }

    /**
     * Immutable container for userinfo results.
     *
     * @param username   resolved username
     * @param attributes flattened/normalized attribute set
     */
    private record CachedUser(String username, AttributeValueSet attributes) {
    }

    /**
     * Retrieve and parse user information from the configured {@code /userinfo} endpoint.
     *
     * <p>Expected payload examples:</p>
     * <ul>
     *   <li>{@code preferred_username}/{@code username}/{@code name}/{@code sub} for the username, and</li>
     *   <li>either {@code attributes.abac_attributes} as an array of {@code "k=v"} strings, or a
     *       structured {@code attributes} object which is flattened into {@code "k=v"} pairs.</li>
     * </ul>
     *
     * @param token the Bearer token value (without the {@code "Bearer "} prefix)
     * @return a {@link CachedUser} if successful; otherwise {@code null} (on non-2xx or any exception)
     */
    private CachedUser fetchUserInfo(String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(userInfoUrl))
                    .timeout(Duration.ofSeconds(5))
                    .header(HttpNames.hAuthorization, "Bearer " + token)
                    .GET().build();

            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                LOG.error("Unable to fetch user info from {}. Received {} : {}", userInfoUrl, response.statusCode(), response.body());
                return null;
            }

            JsonObject j = JSON.parseAny(response.body()).getAsObject();
            String username = tryFields(j, List.of("preferred_name", "username", "name", "sub"))
                    .filter(JsonValue::isString)
                    .map(v -> v.getAsString().value()).orElse(null);

            // Prefer normalised abac_attributes if present; otherwise flatten "attributes" object.
            AttributeValueSet attrSet = null;
            JsonValue attributes = j.get("attributes");
            if (attributes != null && attributes.isObject()) {
                JsonValue abac = attributes.getAsObject().get("abac_attributes");
                // If no explicit ABAC list, process the "attributes" object itself.
                attrSet = flattenToAttributeValueSet(Objects.requireNonNullElse(abac, attributes));
            }
            if (attrSet == null) {
                attrSet = AttributeValueSet.EMPTY;
            }
            return new CachedUser(username, attrSet);
        } catch (Exception e) {
            LOG.error("Unable to fetch/process user info from {}", userInfoUrl, e);
            return null;
        }
    }

    /**
     * Return the first non-null field among the supplied {@code names} from a JSON object.
     *
     * @param obj   JSON object to scan
     * @param names candidate field names in priority order
     * @return the first present {@link JsonValue}, or {@link Optional#empty()} if none
     */
    protected static Optional<JsonValue> tryFields(JsonObject obj, List<String> names) {
        for (String n : names) {
            JsonValue v = obj.get(n);
            if (v != null) return Optional.of(v);
        }
        return Optional.empty();
    }

    /**
     * Legacy attribute-flattening helper (kept for reference and potential reuse).
     * <p>Converts a structured {@link JsonObject} into a list of {@code "k=v"} strings, where:
     * <ul>
     *   <li>arrays become repeated {@code key=value} entries,</li>
     *   <li>objects become {@code key.sub=value},</li>
     *   <li>scalars become {@code key=value}.</li>
     * </ul>
     *
     * @param v a JSON value expected to be an object
     * @return a list of {@code "k=v"} strings, or {@code null} if {@code v} is not an object
     */
    @SuppressWarnings("unused")
    protected static List<String> flattenFromAttributesObject(JsonValue v) {
        if (v == null || !v.isObject()) return null;
        List<String> out = new ArrayList<>();
        JsonObject obj = v.getAsObject();
        for (String key : obj.keys()) {
            JsonValue val = obj.get(key);
            if (val == null) continue;
            if (val.isArray()) {
                JsonArray a = val.getAsArray();
                a.forEach(x -> {
                    String s = jsonScalarAsString(x);
                    if (s != null) {
                        out.add(key + "=" + s);
                    }
                });
            } else if (val.isObject()) {
                JsonObject m = val.getAsObject();
                for (String k : m.keys()) {
                    String s = jsonScalarAsString(m.get(k));
                    if (s != null) {
                        out.add(key + "." + k + "=" + s);
                    }
                }
            } else {
                String s = jsonScalarAsString(val);
                if (s != null) {
                    out.add(key + "=" + s);
                }
            }
        }
        return out;
    }

    /**
     * Build an {@link AttributeValueSet} from either:
     * <ul>
     *   <li>a {@link JsonArray} of {@code "k=v"} strings, or</li>
     *   <li>a {@link JsonObject} treated as a structured set of attributes and flattened to {@code "k=v"}.</li>
     * </ul>
     * Non-conforming entries are ignored.
     *
     * @param v the JSON value to interpret
     * @return a non-null {@link AttributeValueSet} (possibly empty)
     */
    static AttributeValueSet flattenToAttributeValueSet(JsonValue v) {
        if (v == null) return AttributeValueSet.EMPTY;

        List<AttributeValue> out = new ArrayList<>();

        if (v.isArray()) {
            // Expect an array of "k=v" strings
            JsonArray a = v.getAsArray();
            a.forEach(x -> {
                if (x != null && x.isString()) {
                    String s = x.getAsString().value();
                    if (s != null && !s.isBlank()) {
                        AttributeValue value = parseValue(s);
                        if (value != null) {
                            out.add(value);
                        }
                    }
                }
            });
            return AttributeValueSet.of(out);
        }

        else if (v.isObject()) {
            processJsonObject(v.getAsObject(), out);
            return AttributeValueSet.of(out);
        }

        // Anything else can't be interpreted into attributes
        return AttributeValueSet.EMPTY;
    }

    /**
     * Flatten a {@link JsonObject} to a list of {@link AttributeValue}s, applying the rules:
     * <ul>
     *   <li>Arrays: {@code key=[v1,v2]} → {@code key=v1}, {@code key=v2}</li>
     *   <li>Objects: {@code key: {sub: v}} → {@code key.sub=v}</li>
     *   <li>Scalars: {@code key: v} → {@code key=v}</li>
     * </ul>
     * Invalid entries are ignored.
     *
     * @param obj the object to flatten
     * @param out a mutable collection to append results to
     */
    static void processJsonObject(JsonObject obj, List<AttributeValue> out) {
        for (String key : obj.keys()) {
            JsonValue val = obj.get(key);
            if (val == null) continue;

            if (val.isArray()) {
                JsonArray a = val.getAsArray();
                a.forEach(x -> {
                    String s = jsonScalarAsString(x);
                    if (s != null) {
                        AttributeValue value = parseValue(key + "=" + s);
                        if (value != null) {
                            out.add(value);
                        }
                    }
                });

            } else if (val.isObject()) {
                JsonObject m = val.getAsObject();
                for (String k : m.keys()) {
                    String s = jsonScalarAsString(m.get(k));
                    if (s != null) {
                        AttributeValue value = parseValue(key + "." + k + "=" + s);
                        if (value != null) {
                            out.add(value);
                        }
                    }
                }

            } else {
                String s = jsonScalarAsString(val);
                if (s != null) {
                    AttributeValue value = parseValue(key + "=" + s);
                    if (value != null) {
                        out.add(value);
                    }
                }
            }
        }
    }

    /**
     * Parse a single {@code "k=v"} pair into an {@link AttributeValue}, returning {@code null}
     * if the string is not syntactically valid for {@link AE#parseAttrValue(String)}.
     *
     * @param toParse pair in the form {@code "key=value"}
     * @return parsed {@link AttributeValue}, or {@code null} if invalid
     */
    static AttributeValue parseValue(String toParse) {
        try {
            return AE.parseAttrValue(toParse);
        } catch (Exception e) {
            // NOTE: we will ignore any attributes that don't fit the pattern.
            return null;
        }
    }

    /**
     * Flatten an array value under a given base key.
     * <ul>
     *   <li>Nested arrays are processed recursively with the same base key.</li>
     *   <li>Objects delegate to {@link #processNestedObject(String, JsonObject, List)} with {@code baseKey} as prefix.</li>
     *   <li>Scalars become {@code baseKey=value} entries.</li>
     * </ul>
     *
     * @param baseKey prefix to use for generated {@code "k=v"} entries
     * @param arr     array to process
     * @param out     output collection
     */
    private static void processJsonArray(String baseKey, JsonArray arr, List<AttributeValue> out) {
        arr.forEach(elem -> {
            if (elem == null) return;

            if (elem.isArray()) {
                processJsonArray(baseKey, elem.getAsArray(), out);
            } else if (elem.isObject()) {
                processNestedObject(baseKey, elem.getAsObject(), out);
            } else {
                String s = jsonScalarAsString(elem);
                if (s != null) {
                    AttributeValue value = parseValue(baseKey + "=" + s);
                    if (value != null) {
                        out.add(value);
                    }
                }
            }
        });
    }

    /**
     * Flatten a nested object under a given prefix:
     * <ul>
     *   <li>Scalars  → {@code prefix.field=value}</li>
     *   <li>Objects  → recurse with {@code prefix.field} as next prefix</li>
     *   <li>Arrays   → delegate to {@link #processJsonArray(String, JsonArray, List)} using {@code prefix.field}</li>
     * </ul>
     *
     * @param prefix key prefix to apply
     * @param obj    nested object to process
     * @param out    output collection for parsed attribute values
     */
    static void processNestedObject(String prefix, JsonObject obj, List<AttributeValue> out) {
        for (String k : obj.keys()) {
            JsonValue v = obj.get(k);
            if (v == null) continue;

            String nextPrefix = prefix + "." + k;

            if (v.isArray()) {
                processJsonArray(nextPrefix, v.getAsArray(), out);
            } else if (v.isObject()) {
                processNestedObject(nextPrefix, v.getAsObject(), out);
            } else {
                String s = jsonScalarAsString(v);
                if (s != null) out.add(parseValue(nextPrefix + "=" + s));
            }
        }
    }

    /**
     * Convert a scalar {@link JsonValue} to a Java {@link String}.
     * <ul>
     *   <li>Strings return their value,</li>
     *   <li>numbers return {@code toString()},</li>
     *   <li>booleans return {@code "true"} or {@code "false"},</li>
     *   <li>arrays/objects return {@code null}.</li>
     * </ul>
     *
     * @param v JSON scalar
     * @return string representation or {@code null} if not a scalar
     */
    static String jsonScalarAsString(JsonValue v) {
        if (v == null) return null;
        if (v.isString()) return v.getAsString().value();
        if (v.isNumber()) return v.getAsNumber().value().toString();
        if (v.isBoolean()) return Boolean.toString(v.getAsBoolean().value());
        return null; // arrays/objects handled by callers
    }
}
