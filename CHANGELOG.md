# Change Log :: RDF ABAC

## 3.0.0

This is a major version with signficant **BREAKING CHANGES** that will require consumers to adapt their existing code
for:

- Refactored `LabelsStore` interface, notably:
    - All methods that returned or took a `List<Label>` now return/take a singular `Label`
    - Added new `add()` and `remove()` overloads that allow for associating labels with `Quad`'s not just `Triple`'s
    - All existing implementations now treat `Triple` as a `Quad` with `Quad.defaultGraphIRI` as the graph
    - `forEach()` signature changed to take a `Consumer<Quad, Label>`
- Refactored RocksDB backed label stores
    - Existing `LabelsStoreRocksDB` renamed to `LegacyLabelsStoreRocksDB` and moved to package
      `io.telicent.jena.abac.labels.store.rocksdb.legacy`
        - Existing implementation modified to conform to new `LabelsStore` interface
        - Note that some functionality of the new interface (labelling quads outside the default graph) is intentionally
          **NOT** implemented as that isn't possible without breaking backwards compatibility with existing on disk
          stores.
    - Added new `DictionaryLabelsStoreRocksDB` in package `io.telicent.jena.abac.labels.store.rocksdb.modern`
        - This replaces the existing legacy store, and includes support for [automatically
          migrating](#migrating-legacy-rocksdb-stores) legacy stores.
        - Only `StoreFmtByHash` is permitted as the `StoreFmt` for this new store as the other pre-existing store
          formats are considered deprecated
        - This is built upon our [Smart Cache Storage](https://github.com/telicent-oss/smart-cache-storage) libraries
          `LabelsStore` API and wraps it into the RDF-ABAC `LabelsStore` API
    - `StoreFmt` interface refactored to reflect ability to label quads
        - Added `formatLabel()` and `formatQuad()` to `StoreFmt.Encoder` interface
        - Added `parseQuad()` and `parseLabel()` to `StoreFmt.Parser` interface
        - `StoreFmtByNodeId` marked as deprecated, and for removal, no persistent node table was ever implemented so
          this was **NEVER** suitable for production usage
        - `StoreFmtByString` marked as deprecated since it offers very inefficient storage utilisation
        - Added explicit store format tracking into the on-disk database so we can detect format mismatches at startup
          and refuse to run.  Otherwise mismatched store formats could lead to labels not being properly retrieved and
          applied.
        - `Hasher` implementations have better `toString()` implementations to ensure they all return a unique name.
          Previously their names were based on the hash function implementation, for some hash functions which offer
          multiple hash lengths the implementation class was the same.  Thus the hashers could not be uniquely
          identified and the above store format check would incorrectly pass.
- Removed all remaining vestiges of deprecated pattern matching for labels 
    - **NB** Pattern based wildcard labelling was already disabled for a long time
    - Removed `LabelsStoreMemPattern`
    - Removed pattern based logic from other `LabelsStore` implementation
    - i.e. a `Quad`/`Triple` **MUST** be labelled precisely, and is otherwise considered not labelled
- Labels Graph changes:
    - The RDF serialization of a labels graph has been updated as follows:
        - A pattern, the object of an `authz:pattern` triple **MAY** now have an optional 4th token to indicate the
          named graph for the quad that the pattern applies to e.g. `[] authz:pattern 'ex:graph ex:subject ex:predicate
          ex:object'`.  Existing labels graphs which have patterns containing only three tokens are treated as declaring
          labels for quads in the default graph.
        - Each pattern **MUST** have one, and only one, `authz:label` triple associated with it.  It is no longer
          permitted to have multiple `authz:label` triples associated with a single pattern.  If existing label graphs
          have this then those labels should be appropriately combined, e.g., `[] authz:label "employee" ; authz:label
          "admin"` should become `[] authz:label "employee && admin"`
        - The object of an `authz:label` triple may now be an `xsd:base64Binary` literal if a graph needs to encode a
          label that cannot be safely persisted directly in the RDF syntax being used.
        - The value of an `authz:label` is no longer required to be a valid RDF-ABAC label.  This permits a label graph
          to encode labels in other labelling schemes.

### 3.x Migration Guide

#### General API Usage

The main changes, as noted above, are in method signatures around assigning and retrieving labels.  Where you previously
would pass in/get returned multiple labels i.e. `List<Label>` you will now pass in/get returned a singular `Label`.  The
other main signature change is that where you would previously have called `labelsForTriple(Triple)` to retrieve labels
for a given `Triple` you should now be calling `labelForQuad(Quad)` instead.

#### Label Graph Usage

If you are using RDF label graphs to declare fine-grained labels for data then you **MAY** need to make some changes.
Firstly if you have any graphs where you declare multiple `authz:label` triples for a single pattern e.g.

```
[] authz:pattern 'ex:subject ex:predicate ex:object' ;
   authz:label "employee" ; 
   authz:label "admin" .
```

Then you will need to combine the multiple labels into a single label e.g.

```
[] authz:pattern 'ex:subject ex:predicate ex:object' ;
   authz:label "employee && admin" .
```

Secondly if you wish to start labelling quads instead of triples your `authz:pattern` values **MUST** be updated to
insert the graph name as the first token of the pattern e.g.

```
[] authz:pattern 'ex:graph ex:subject ex:predicate ex:object' ;
   authz:label "employee && admin" .
```

#### RocksDB Storage Usage

If you were creating a `LabelsStoreRocksDB` directly then you will need to change the class and package to
`LegacyLabelsStoreRocksDB`.  If you were creating this storage indirectly, e.g., via an RDF configuration file, then
existing configuration continues to work as-is for the time being.

If you continue to use the legacy store then some functionality will produce errors e.g. trying to add/retrieve a label
for a `Quad` outside of the default graph as the existing on-disk store formats are not able to store quad to label
mappings.

##### Using the new RocksDB store

For users of RDF configuration files you can set the new `authz:labelStoreLegacy` property to
`false` if you wish e.g.

```ttl
<#datasetAuth> rdf:type authz:DatasetAuthz ;
    authz:labelsStore [
      authz:labelsStorePath "/path/to/label-store/" ;
      # Disable the legacy mode store in favour of the modern store
      authz:labelsStoreLegacy false ;
      # Configure the desired hash function for the modern store
      authz:labelsStoreByHash true ;
      authz:labelsStoreByHashFunction "xx128"
    ] ;
    authz:dataset :basedata ;
    authz:authServer true 
    .
```

In future releases we will change the default to automatically create the new store rather than the legacy store but in
the interests of allowing users time to migrate we have not done that in the `3.0.0` release.

##### Migrating legacy RocksDB stores

**IMPORTANT** This migration is a one way one time process after which the legacy column families will be dropped, you
**MUST** take a backup of your existing legacy store before proceeding with this procedure if you want to be able to
revert the migration.

Firstly you need to determine what `StoreFmt` you are currently using as to whether migration is supported:

| Legacy `StoreFmt`  | Migration Supported                          |
|--------------------|----------------------------------------------|
| `StoreFmtByString` | Yes                                          |
| `StoreFmtByHash`   | Yes, configured hash function **MUST** match |
| `StoreFmtByNodeId` | No, format never properly supported          |

In order to migrate simply open the RocksDB database with the new `DictionaryLabelsStoreRocksDB` implementation, and if
using `StoreFmtByHash` ensuring the hash function matches that used for the legacy database.  This will automatically
detect the pre-existing legacy format data and begin migration.  This migration is atomic, transactional and safe
against interruptions, i.e. if your process is terminated during migration then the migration will resume where it left
off upon next store open.  Progress is reported to logs during migration including a percentage indicator.

For uses of RDF configuration files you can set the new `authz:labelsStoreLegacy` property to `false` to instruct the
assembler layer to open your database using the new store implementation.

As already noted once migration has started you will not be able to open your database with the
`LegacyLabelsStoreRocksDB` anymore.  Therefore ensure you take a backup of your existing database prior to starting the
migration.

## 2.0.3

- Build improvements:
    - Rotated Maven signing key

## 2.0.2

- RDF-ABAC label syntax now permits numbers to be used as attribute names
- Build improvements:
    - RocksDB upgraded to 10.5.1
    - Various build and test dependencies upgraded to latest available

## 2.0.1

- Build improvements:
  - Upgrading Jetty (CVE-2026-1605)

## 2.0.0

- Build improvements:
    - **BREAKING** Minimum Java version is now 21
    - Apache Commons FileUpload upgraded to 2.0.0-M5
    - Apache Jena upgraded to 6.0.0
    - Log4j2 upgraded to 2.25.3
    - Various build and test dependencies upgraded to latest available


## 1.1.4

- Added OS level to `clearance` and `classification` attribute hierarchies

## 1.1.3

- Added hardcoded fallback for `clearance` attribute hierarchy
- Build improvements:
    - Apache Commons Lang upgraded to 3.20.0
    - Various build and test dependencies upgraded to latest available

## 1.1.2

- Fixes for Auth Server integration.
- Fixes for loading data labels for testing. 

## 1.1.1

- Fixed a bug when loading a `DatasetAuthz` dataset from an RDF configuration file that used the new `authz:authServer` property introduced in 1.1.0
- Added hardcoded fallback for `classification` attribute hierarchy
- Build improvements:
    - Upgraded Apache Jena to 5.6.0
    - Upgraded various build and test dependencies to latest available

## 1.1.0

- Added support for obtaining user attributes from an OAuth2/OIDC compliant servers `/userinfo` (or equivalent) endpoint as `AttributesStoreAuthServer`
    - Added new `UserInfoEnrichmentFilter` for augmenting incoming requests with retrieved user info
- Made operation constants in `ServerABAC.Vocab` public
- Build improvements:
    - Upgraded Apache Commons FileUpload to 2.0.0-M4
    - Upgraded Apache Jena to 5.5.0
    - Upgraded Guava to 33.5.0-jre
    - Upgraded Log4j to 2.25.2
    - Upgraded various build and test dependencies to latest available

## 1.0.2
- Build improvements:
  - Upgraded RocksDB to v10.2.1

## 1.0.1

- Reverted `SysAbac.hSecurityLabel` constant type to a `String`
- Build improvements:
    - Upgraded Apache Commons BeanUtils to 1.11.0
    - Upgraded various build and test dependencies to latest availableq

## 1.0.0

- **BREAKING** Changed security labels from being a plain `String` to being an immutable `Label` class wrapping a byte
  array and `Charset`.
- Build improvements:
    - Upgraded Apache Jena to 5.4.0
    - Upgraded SLF4J to 2.0.17
    - Added some performance benchmarks to better asses proposed improvements
    - Upgraded various build and test dependencies to latest available

## 0.73.4 
- Fixed bug introduced in (0.73.3) for default label processing. 

## 0.73.3
- Further improvements to Blank Node processing. 

## 0.73.2
- Correcting issue with Blank Node processing.

## 0.73.1

- Refactored restore operation. 

## 0.73.0

- Reinstated 2 argument constructor for `AttributesStoreRemote` that was unintentional breaking change in previous
  release
- Upgraded Log4j to 2.24.2
- Upgraded various build and test dependencies to latest available

## 0.72.1

- Adding backup/restore RocksDB functionality.

## 0.72.0

- Upgraded Apache Jena to 5.2.0
- Removed some no longer needed exclusions

## 0.71.10

- Upgrade Protobuf to 4.28.2
- Upgraded various dependencies to latest available
- Refactor POM to be consistent with other Telicent repositories
- Improved documentation for new users

## 0.71.9

- Upgrade Protobuf to 4.27.5

## 0.71.8

- **Experimental:** Added support for using a hash function to encode keys for RocksDB Labels Store

## 0.71.7

- Further increased byte buffer size when encoidng keys for RocksDB Labels Store

## 0.71.6

- Bug fix for an upstream Jena issue around maximum lock count exceeded

## 0.71.5

- Increased byte buffer size used when encoding keys for RocksDB Labels Store

## 0.71.4

- Add dependency exclusions to fix a JUnit dependency that was unintentionally 
  leaked into compile scope by one of our dependencies
- Fixes a packaging error that prevented publishing to Maven Central, therefore
  this release includes the following content intended for a prior release:

### 0.71.3

- Removed never implemented expression language features from documentation
- Build improvements:
    - Apache Jena upgraded to 5.1.0
    - Log4J upgraded to 2.23.1
    - RocksDB upgraded to 9.4.0
    - SLF4J upgraded to 2.0.13
    - Various build and test dependencies upgrade to latest available


## 0.71.2

- First release to Maven Central

## 0.71.1

- Bug fix around transaction promotions when used ABAC is configured with a RocksDB Label Store

## 0.71.0

- Assorted bug fixes and dependency updates

## 0.70.0

- Prepare for open source.

## 0.60.0

- Improve in-memory LabelsStore, now called LabelsStoreMem
- Same policy for bad labels and duplicates labels across in-memory and
  persistent labels stores. Labels are checked before being stored.
- Remove support for labels persistence in a triple store.

## 0.50.1

- Bug fix
 
## 0.50.0 

- RocksDB back LabelsStore.

## 0.20.0

- Change to "replace" policy for a duplicate label for a triple
- Include RocksDB label store code (not active)
