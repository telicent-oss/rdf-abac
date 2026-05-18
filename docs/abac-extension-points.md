# RDF-ABAC Extension Points

By default, RDF-ABAC is deployed with sensible defaults but also exposes well-defined extension points so
that downstream systems can plug-in more nuanced decision logic that is better applicable to their needs  
without having to extend every operation class.

## Contents

* [Dataset Filter Provider](#dataset-filter-provider)
  * [Default implementation](#default-implementation)
  * [Installing a custom provider globally](#global-custom-provider)
  * [Installing a per-dataset override](#dataset-custom-provider)
  * [Sample example](#sample-example)
  * [Order of Precedence](#Order-of-Precedence)

## Dataset Filter Provider

Dataset Filter Provider is the extension point used by `ABACRequest.filterDataset(...)`. 

Every ABAC-aware operation (SPARQL query, GSP read, SHACL validation, GraphQL, ...) ultimately delegates to `ABACRequest.filterDataset(...)`
to produce the per-request view of a `DatasetGraphABAC`, so plugging in a custom provider is a single point of customisation for the whole request path.

The interface has two methods:

```java
public interface DatasetFilterProvider {
    DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt);
    DatasetGraph filterDataset(DatasetGraph dsgBase, LabelsStore labels,
                               Label defaultLabel, CxtABAC cxt);
}
```

The first form is what the request path calls; the second is the lower-level
overload used by programmatic APIs and tests that don't have a
`DatasetGraphABAC` to hand.

### Default Implementation

The built-in default is `io.telicent.jena.abac.DefaultDatasetFilterProvider`. It
reproduces the historical behaviour as such:

* Uses the `LabelsStore` from the `DatasetGraphABAC` to build a `QuadFilter` that applies RDF-ABAC label filtering.
* Returns a `DatasetGraphFilteredView` over the underlying `DatasetGraph` that exposes all named graphs of the underlying dataset.

If no custom provider is installed, both the global and per-dataset resolution paths resolve to this default and behaviour is identical to previous releases.

### Global Custom Provider

Example code:
```java
// Set the new provider as global
ABACRequest.setDatasetFilterProvider(new GlobalDatasetFilterProvider());
// To return to previous behavior
ABACRequest.resetDatasetFilterProvider(); // restore the default
```

### Dataset Custom Provider

Different `DatasetGraphABAC` instances can have different filtering strategies.

Example code:
```java
DatasetGraphABAC dsgAuth = ABAC.authzDataset(base, labels, defaultLabel, attrs);
dsgAuth.setFilterProvider(new DatasetSpecificFilterProvider());
```

A per-dataset provider, when set, takes precedence over the global provider for
calls to `ABACRequest.filterDataset(DatasetGraphABAC, CxtABAC)` that target the given
dataset. Passing `null` to `setFilterProvider` clears the override and
falls back to the global provider.

### Sample Example

The motivating use case for this extension point is dynamically deciding which
subset of the named graphs of a dataset is visible to a given request. A
custom provider has access to the full `CxtABAC` (and therefore the user's
attributes) and can build its own `Collection<Node>` of named graphs to pass
through to `DatasetGraphFilteredView`:

```java
public class NamedGraphRestrictingProvider extends DefaultDatasetFilterProvider {
    @Override
    public DatasetGraph filterDataset(DatasetGraphABAC dsgAuthz, CxtABAC cxt) {
        DatasetGraph base = dsgAuthz.getData();
        QuadFilter quadFilter = buildQuadFilter(dsgAuthz, cxt);   // re-use label filtering
        Collection<Node> visibleGraphs = decideVisibleGraphs(base, cxt);
        return new DatasetGraphFilteredView(base, quadFilter, visibleGraphs);
    }
}
```

Implementations are free to ignore the labels store entirely, combine multiple
filter strategies, return a completely different `DatasetGraph` wrapper, or
delegate back to `ABAC.DEFAULT_DATASET_FILTER_PROVIDER` for the cases they
don't want to customise.

### Order of Precedence

When `ABAC.filterDataset(DatasetGraphABAC dsgAuth, CxtABAC cxt)` is called the
provider used is, in order of precedence:

1. The per-dataset provider on `dsgAuth` (if any non-null one is set via
   `DatasetGraphABAC.setFilterProvider`).
2. The globally registered provider (`ABAC.getDatasetFilterProvider()`).
3. `ABAC.DEFAULT_DATASET_FILTER_PROVIDER` (the initial value of the global
   provider).

The lower-level `ABAC.filterDataset(DatasetGraph, LabelsStore, Label, CxtABAC)`
overload only consults steps 2 and 3, because no `DatasetGraphABAC` is in scope
to carry a per-dataset override.
