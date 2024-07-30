# Change Log :: RDF ABAC

## 0.71.4

- Add dependency exclusions to fix a JUnit dependency that was unintentionally 
  leaked into compile scope by one of our dependencies

## 0.71.3

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
