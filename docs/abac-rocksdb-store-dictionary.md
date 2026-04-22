# RocksDB Dictionary Store

In `3.0.0` we introduced a new `DictionaryLabelsStoreRocksDB` implementation of the `LabelsStore` interface, this
replaces what is now known as the [`LegacyLabelsStoreRocksDB`][Legacy].  This page describes more about the internals of
how that store is implemented and compares to the existing store.

The key differences versus the legacy store are as follows:

- The new store can associate labels with Quads, not just Triples.
    - Some of the legacy [store formats](legacy/abac-rocksdb-store-format.md) are incompatible with storing Quads and in
      order to retain backwards compatibility of the legacy store the legacy implementation continues to only support
      labelling Triples.
    - This permits the same Triple appearing in different named graphs, and thus being different Quads, to each have
      unique labels.
- The new store uses a dictionary encoding whereby each unique label byte sequence is given a unique internal ID in the
  form of a `long`.
    - For scenarios where label variety is limited this vastly decreases storage utilisation.
    - This also allows the store to be forwards compatible with future label formats beyond RDF-ABAC.
- Only [`StoreFmtByHash`](abac-rocksdb-label-store-hash.md) is supported.
    - This provides predictable fixed key size for quads and again vastly decreases storage utilisation particularly for
      users with large literals in their data.
- The new store uses the full [Transaction](#transactions) capabilities of RocksDB and thus is safer to operate in
  production.
- The new store can [automatically](#legacy-storage-migration) migrate data from the legacy store.

More detail on the various internal implementation aspects can be found below:

- [Storage Layout](#storage-layout)
- [Transactions](#transactions)
- [Legacy Storage Migration](#legacy-storage-migration)

## Storage Layout

The new storage format consists of 5 column families:

- The `default` column family used to store metadata about the store itself.
- The `labels_to_ids` column family used to map each unique label to its corresponding internal ID in the store.
- The `ids_to_labels` column family which is the reverse lookup for the above.
- The `keys_to_labels` column family which maps quads to their internal label ID.
- The `counters` column family which stores persistent counters for the store.

### `default` column family

As noted this is used purely to store metadata about the store for internal bookkeeping and validation purposes.  This
includes the Store Format itself, which **MUST** always be some variant of
[`StoreFmtByHash`](abac-rocksdb-label-store-hash.md), as well as metadata about [migration](#legacy-storage-migration)
from the [legacy][Legacy] store format.

### `counters` column family

This column family stores persistent counters, currently there is a single counter key `next_label_id` that records the
next available internal label ID.

### `labels_to_ids` and `ids_to_labels` column families

These column families store the mappings from labels to internal label IDs and vice versa.  As the name of the new store
implies this is effectively a dictionary encoding, each unique label is assigned a unique internal ID.  For the
`labels_to_ids` column family the keys are the original label byte sequence, and the values are the little endian
encoding of the `long` ID assigned for the label.  The `ids_to_labels` is the reverse lookup so the keys are the little
endian encoding of the `long` ID assigned for a label, and the value is the original label byte sequence.

> **NB** Unlike the [legacy][Legacy] store the labels are not restricted to be valid [ABAC
> expression](abac-specification.md) i.e. this store is forwards compatible with alternative future label formats.

### `keys_to_labels` column family

This column family associates RDF Quads with an internal label ID.  The keys are a
[hash](abac-rocksdb-label-store-hash.md) of the quad and the value is the little endian encoding of the `long` ID for
the quads label.

Note that this means obtaining the original label for a quad in order to evaluate access decisions requires a two step
lookup in the RocksDB storage.  To ameliorate the performance impact of this a large cache is used internally in the
implementation that maps directly from `Quad` to `Label`, thus frequently accessed quads will have their labels
immediately available without needing to access the RocksDB storage.

## Transactions

Internally the new store uses the Rocks `TransactionDB` API to provide fully isolated ACID transactions.  This ensures
that inserting a label for a Quad, which may also require allocating it a new internal ID, inserting it into the
`labels_to_ids` and `ids_to_labels` column families, updating the persisted `counters` etc. is a reliable and durable
operation.

> **NB** RocksDB transactions **DO NOT** provide deadlock protection.  Therefore this store **MUST** only be used in
> MR+SW (Multiple Reader plus Single Writer) scenarios.
> 
> If used with our [Smart Cache Graph][SCG] service then this is enforced by how datasets are configured and accessed.

## Legacy Storage Migration

The new store includes a forward migration path for stores created using the [legacy][Legacy] store providing that one of the supported Store Formats was used for the legacy store:

| Legacy `StoreFmt`  | Migration Supported                          |
|--------------------|----------------------------------------------|
| `StoreFmtByString` | Yes                                          |
| `StoreFmtByHash`   | Yes, configured hash function **MUST** match |
| `StoreFmtByNodeId` | No, format never properly supported          |

When a database location containing legacy data is opened for the first time an automated data migration process is
triggered.

> **NB** This automatic migration is non-reversible, therefore we would recommend taking a backup of your store prior to
> the migration in case you wish to revert to using the legacy store.

The legacy migration involves reading data from the legacy `CF_ABAC_SPO` column family and inserting it into the new
column families appropriately.  This process can take some time and is triggered in the constructor so that other code
cannot utilise the store until the migration completes.

Data migration happens [transactionally](#transactions) in batches of 1 million quad to label mappings.  During the
migration process we store several metadata keys in the `default` column family that track and checkpoint progress.
Should the migration be interrupted, e.g. the service hosting the store is interrupted/killed during migration, then
upon next reopening the store the partial data migration is automatically resumed from the last committed
checkpoint.

Progress logging, including a percentage indicator, is provided throughout the migration process, and this remains
accurate even across partial migrations thanks to the aforementioned tracking metadata. Migration can be a long
operation depending on the size of the legacy store, the actual time required for the migration will depend on several
factors such as existing store format, number of quad to label mappings present, underlying filesystem type etc.

Upon successful completion of the migration process the legacy `CF_ABAC_SPO` column family is dropped in order to
reclaim the space that is no longer needed.  Our internal testing and benchmarking suggests that for our typical RDF
data the new store uses approximately half the storage space of the legacy store.  For data with lots of large URIs and
literals the storage space reductions should be even greater.

[Legacy]: legacy/abac-rocksdb-label-store.md
[SCG]: https://github.com/telicent-oss/smart-cache-graph

