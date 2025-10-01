# RDF-ABAC : Attribute Value Hierarchies

Attribute Value Hierarchies provide a way to grant a user an attribute value permission
that has the idea that it also grants lesser permission.

Hierarchies are defined and managed by the [User Attribute Store](abac-user-attribute-store.md).

Suppose we have the hierarchy for the attribute `status`:

```
[] authz:hierarchy [ authz:attribute "status" ;
                     authz:attributeValues "public, confidential, sensitive, private" ];
```
The list is written in least-most restrictive order.
"public" is the least restrictive, "private" the most restrictive.


If the data triple has the label
```
    status=public
```

then a user with attribute value `status=confidental` can see that data triple.

## Fetching hierarchies from Auth Server

If your organization centralizes attribute hierarchies in the Auth Server, expose
an endpoint such as:
```
GET /hierarchy/{attr}
Accept: application/json
```

Example response:
```json
{
  "uuid": "6593c90a-dd68-3437-9e0b-b5a69c816dc1",
  "name": "clearance",
  "tiers":  ["TS", "S", "O", "U"],
  "levels": ["TS", "S", "O", "U"]
}
```

Configure the ABAC dataset with:
```
authz:hierarchiesURL "http://auth.telicent.localhost:9000/hierarchy/{name}" ;
```

The ABAC engine will call this endpoint for the specific attribute name and build
a Hierarchy instance. If you instead set a local RDF file, it loads all hierarchies
eagerly at startup.

