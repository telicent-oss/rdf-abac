# Change Log :: RDF ABAC

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
