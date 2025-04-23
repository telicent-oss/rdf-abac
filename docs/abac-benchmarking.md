# Benchmarking

## Introduction
The rdf-abac-benchmarking module is to allow us to carry out performance testing on specific sub-modules within our code. 

Note: this should be only used as a guide. Local runs can be impacted by any number of things - how many other apps are running on the device etc...; so we cannot put too much faith in it. 

## Build the Benchmarking jar
This will build the benchmarks jar 
```bash
mvn package
```
## Run Benchmarking jar
It is recommended to store the output to file. 

*Note:* this can take a significant time to run.
```bash
java -jar target/benchmarks.jar 2>/dev/null | tee run_output.txt
```

## Parsing Results
This is a simple python script that reduces the output down to more minimal, useful information. 
```bash
python3 parse_output.py run_output.txt
```

## Examples
To illustrate the run, we have LabelsStoreRocksDBBenchmark, which populates a Rocks DB instance with 1M entries.
The Benchmark then fetches 1M labels in a random order.

### Baseline
Here is a summary of a run with the existing set-up focused on this code in particular. 

As illustrated, the method does not add the new entry to the cache but instead invalidates it ahead of updating the underlying Rocks DB.
```java
public void add(Triple triple, List<String> labels) {
    Triple triple2 = tripleNullsToAnyTriple(triple);
    tripleLabelCache.remove(triple2);
    Node s = triple2.getSubject();
    Node p = triple2.getPredicate();
    Node o = triple2.getObject();
    addRule(s, p, o, labels);
}
```
After running and processing the output, we get the following score. 
```json
{"benchmark": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBenchmark.test_labelFetch", "score": 229.421, "error_percent": 99.9, "error": 20.367, "min": 189.862, "avg": 229.421, "max": 282.23}
```

### First change
Here is a follow-up with a slightly improved approach - instead of only removing cached entries when updating entries, we re-add the entry to the cache. 
You can see it has a slightly lower (and thus better) score. 
```json
{"benchmark": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBenchmark.test_labelFetch", "score": 203.339, "error_percent": 99.9, "error": 11.021, "min": 184.817, "avg": 203.339, "max": 230.661}
```

### Final change
```java
public void add(Triple triple, List<String> labels) {
        Triple normalizedTriple = tripleNullsToAnyTriple(triple);
        List<String> cachedLabels = tripleLabelCache.getIfPresent(normalizedTriple);
        if (cachedLabels != null && CollectionUtils.isEqualCollection(labels, cachedLabels)) {
            // Labels are the same, no need to update
            return;
        }
        // Remove the old entry if it exists
        if (cachedLabels != null) {
            tripleLabelCache.remove(normalizedTriple);
        }
        // Add the new triple and update the cache, if a previous entry or cache is under-populated.
        addRule(normalizedTriple.getSubject(), normalizedTriple.getPredicate(), normalizedTriple.getObject(), labels);
        if (cachedLabels != null || (tripleLabelCache.size() < LABEL_LOOKUP_CACHE_SIZE) ) {
            tripleLabelCache.put(normalizedTriple, labels);
        }
    }
```
Again, a further update. This time we go one step further, immediately returning if we already have the same labels and also updating the cache if not full.
The score is again further improved.
```json
{"benchmark": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBenchmark.test_labelFetch", "score": 159.146, "error_percent": 99.9, "error": 6.292, "min": 149.584, "avg": 159.146, "max": 185.354}
```

## Profiling 

In theory, JMH can be made to work with certain profilers. In reality, due to limitations with permissions and OS, they are not able to be used.

### GC
This profile tracks and assesses the Garbage Collection.
```bash
java -jar target/benchmarks.jar -prof gc -f 1 -wi 5 -i 5
```
### Stack
As the name suggests this provides analysis on the performance of the stack.

```bash
java -jar target/benchmarks.jar -prof stack -f 1 -wi 5 -i 5
```
### Java Flight Recorder
This leverages the JFR analysis, creating a profile.jfr file for each benchmark. 
```bash
java -jar target/benchmarks.jar -prof jfr -f 1 -wi 5 -i 5
```

## Benchmarking classes
### Labels Store Rocks DB Benchmark
As mentioned above, this was created to illustrate some improvements to the local caching in the Rocks DB Label Store. Note the package matches so that we can make use of the constructor without trickery.

### Dataset Graph ABAC Benchmark
This just creates an ABAC Graph (leveraging some the Label Store work) and tests adding and querying of the underlying graph.