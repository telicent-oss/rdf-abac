# Auth Server Integration

RDF-ABAC can integrate with an external **Auth Server** to enrich incoming requests with user attributes fetched from the Auth Server’s `/userinfo` endpoint. It can also (optionally) fetch **attribute hierarchies** from the Auth Server via `/hierarchy/{name}`.

This document explains how to enable that flow, what the endpoints should return, and how to operate and troubleshoot the setup.

---

## Overview

- **User attributes**: Read from `Authorization: Bearer <JWT>` using `GET /userinfo`.
    - The Fuseki runtime adds a filter (`UserInfoEnrichmentFilter`) that calls `/userinfo` and caches the result per token/username.
- **Hierarchies** (optional): Resolved by the ABAC engine via `authz:hierarchiesURL`.
    - Can be a **local RDF file** _or_ a **remote HTTP(S) template** like `http://auth…/hierarchy/{name}`.

There are three bearer modes:

- `legacy`   — only the legacy bearer format (e.g., `Bearer user:alice`) is processed.
- `hybrid`   — **default**; if a JWT is present, call `/userinfo`, else fall back to the legacy filter.
- `userinfo` — require a valid JWT and call `/userinfo` (401 on missing/invalid token).

---

## Configuration

### 1) Select the attribute store (Auth Server mode)

In your dataset configuration TTL, choose the Auth Server–backed store:

```turtle
:abacConf a authz:ABAC ;
    authz:dataset     :dataset ;
    authz:authServer  true ;
    # Optional hierarchies (see next section)
    # authz:hierarchiesURL "http://auth.telicent.localhost:9000/hierarchy/{name}" ;
    # or
    # authz:hierarchiesURL <file:attribute-hierarchies.ttl> ;
    .
```

> **Important:** Choose **exactly one** of:
> - `authz:authServer true` (this mode),
> - `authz:attributes` (local RDF file store), **or**
> - `authz:attributesURL` (remote legacy store).
>
> Setting more than one will be rejected.

### 2) Configure bearer processing mode

Set the bearer mode via **system property** (recommended for tests) or **environment variable**:

```bash
# system property (recommended for tests):
-DABAC_AUTH_SERVER_MODE=hybrid

# or environment variable:
ABAC_AUTH_SERVER_MODE=hybrid
```

Valid values: `legacy`, `hybrid` (default), `userinfo`.

### 3) Point the filter at your `/userinfo` endpoint

`UserInfoEnrichmentFilter` uses `ABAC_USERINFO_URL`:

```bash
# system property
-DABAC_USERINFO_URL=http://auth.telicent.localhost:9000/userinfo

# or environment variable
ABAC_USERINFO_URL=http://auth.telicent.localhost:9000/userinfo
```

### 4) (Optional) Configure hierarchies

Tell the ABAC engine where to get attribute hierarchies:

- **From Auth Server (on demand):**
  ```turtle
  authz:hierarchiesURL "http://auth.telicent.localhost:9000/hierarchy/{name}" ;
  ```

- **From local RDF file (loaded at startup):**
  ```turtle
  authz:hierarchiesURL <file:attribute-hierarchies.ttl> ;
  ```

If `authz:hierarchiesURL` is omitted, **no hierarchies** are applied.

---

## Auth Server contracts

### `/userinfo`

- **Method**: `GET`
- **Auth**: `Authorization: Bearer <JWT>`
- **Response**: JSON object that includes a username and attributes.

RDF-ABAC extracts the username from the **first present** of:
`preferred_username`, `username`, `name`, `sub` (must be a **string**).

Attributes can be provided in either of two forms:

1) **Array of normalized strings** under `attributes.abac_attributes`:
   ```json
   {
     "preferred_username": "A067189",
     "attributes": {
       "abac_attributes": [
         "clearance=TS",
         "compartment.alpha=true",
         "country=GB"
       ]
     }
   }
   ```

2) **Object to be flattened** under `attributes`:
   ```json
   {
     "preferred_username": "A067189",
     "attributes": {
       "clearance": "TS",
       "compartment": { "alpha": true, "beta": false },
       "country": ["GB","US"]
     }
   }
   ```
   This is flattened to `clearance=TS`, `compartment.alpha=true`, `compartment.beta=false`, `country=GB`, `country=US`.  
   (Numbers/booleans are converted to strings; nested arrays/objects are traversed.)

- **Success**: any 2xx status + valid JSON as above.
- **Failure**: non-2xx (e.g. 401) => RDF-ABAC:
    - **hybrid**: falls back to legacy bearer filter (if present),
    - **userinfo**: request will be unauthorized.

### `/hierarchy/{name}` (optional)

- **Method**: `GET`
- **Accept**: `application/json`
- **Response** (example):
  ```json
  {
    "uuid": "6593c90a-dd68-3437-9e0b-b5a69c816dc1",
    "name": "clearance",
    "tiers":  ["TS","S","O","U"],
    "levels": ["TS","S","O","U"]
  }
  ```
  The ABAC engine consumes the ordered **levels** (alias **tiers** for compatibility) to create a `Hierarchy` for the attribute.

- **404**: treated as “no hierarchy for this attribute”.

---

## Caching & performance

RDF-ABAC caches:
- **Token → username** and **username → AttributeValueSet** (User Info cache)

Configure via env or system properties:

```bash
# default 60 seconds
ABAC_USERINFO_CACHE_TTL_SECONDS=60

# default 10000 entries
ABAC_USERINFO_CACHE_MAX_SIZE=10000
```

> Values are honored whether set as environment variables or JVM `-D` properties.

---

## End-to-end examples

### Starting Fuseki with Auth Server integration

```bash
java   -DABAC_AUTH_SERVER_MODE=hybrid   -DABAC_USERINFO_URL=http://auth.telicent.localhost:9000/userinfo   -jar rdf-abac-fuseki-server/target/rdf-abac-fuseki-server-<VERSION>.jar   path/to/your-config.ttl
```

### Querying with a JWT

```bash
curl -sS -X POST 'http://localhost:3030/securedDataset/query'   -H 'Accept: application/sparql-results+json'   -H 'Content-Type: application/sparql-query'   -H "Authorization: Bearer $JWT"   --data 'SELECT (COUNT(*) AS ?count) WHERE { ?s ?p ?o }'
```

### Hierarchy fetch (manual check)

```bash
curl -sS 'http://auth.telicent.localhost:9000/hierarchy/clearance'   -H 'Accept: application/json'
```

---

## Troubleshooting

- **401 `invalid_token`** from `/userinfo`  
  Check:
    - The JWT `alg` matches keys in JWKS (e.g., RS256 vs ES256).
    - The JWT’s `kid` exists in the JWKS set.
    - `aud` and `iss` are what your Auth Server expects.
    - Clock skew (check `exp`, `nbf`).

- **No `/userinfo` traffic visible in Auth Server logs**
    - Client didn’t send `Authorization` header.
    - You’re in `legacy` mode.
    - The dataset path is not protected by the bearer filter (verify your FMod order and dataset endpoints).

- **404 for `/hierarchy/{name}`**
    - ABAC continues without a hierarchy for that attribute.
    - If you intended to use local hierarchies, make sure `authz:hierarchiesURL` points to a valid file path/`file:` URI.

- **Malformed attribute strings**
    - If using the normalized array form, entries must look like `key=value`. Invalid pairs are ignored.

- **Performance**
    - Increase `ABAC_USERINFO_CACHE_TTL_SECONDS` and/or `ABAC_USERINFO_CACHE_MAX_SIZE` for high-throughput workloads.
    - Ensure your Auth Server and Fuseki are on a low-latency path (or colocated).

---

## Security notes

- **HTTPS** strongly recommended for all Auth Server traffic.
- Limit scope of tokens (`aud`, `scope`) to only what `/userinfo` needs.
- If enabling `userinfo` mode (JWT required), deploy appropriate JWKS validation in the Auth Server and ensure Fuseki is not exposed without a reverse proxy if that’s your standard.

---

## Backward compatibility & migration

- `legacy` mode preserves existing `Bearer user:<name>` behavior.
- `hybrid` mode is a safe on-ramp: JWTs get enriched via `/userinfo`; non-JWT requests still work as before.
- To enforce JWTs, switch to `userinfo` mode when ready.

---

## Reference (implementation touchpoints)

- Filter: `io.telicent.jena.abac.fuseki.server.UserInfoEnrichmentFilter`
- Store:  `io.telicent.jena.abac.core.AttributesStoreAuthServer`
- Module: `io.telicent.jena.abac.fuseki.server.FMod_BearerAuthFilter`

These components are wired so that the filter enriches requests (per mode), the store serves user attributes from the in-JVM cache, and hierarchies are resolved from local RDF or the remote Auth Server, depending on `authz:hierarchiesURL`.
