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
