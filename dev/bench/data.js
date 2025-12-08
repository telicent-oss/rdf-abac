window.BENCHMARK_DATA = {
  "lastUpdate": 1765181815106,
  "repoUrl": "https://github.com/telicent-oss/rdf-abac",
  "entries": {
    "LabelsStoreRocksDBBaselineBenchmark": [
      {
        "commit": {
          "author": {
            "name": "Rob Vesse",
            "username": "rvesse",
            "email": "rob.vesse@telicent.io"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "94c833a21232a31c73e976fd0916e156e8add1e6",
          "message": "Merge pull request #163 from telicent-oss/minor/extend_benchmarking\n\n[Minor] Extend benchmarking",
          "timestamp": "2025-12-05T11:19:20Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/94c833a21232a31c73e976fd0916e156e8add1e6"
        },
        "date": 1764934787930,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 135.86649507472237,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 429.47996364251213,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 217.02419577626407,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 592.9211947353383,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Rob Vesse",
            "username": "rvesse",
            "email": "rob.vesse@telicent.io"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "94c833a21232a31c73e976fd0916e156e8add1e6",
          "message": "Merge pull request #163 from telicent-oss/minor/extend_benchmarking\n\n[Minor] Extend benchmarking",
          "timestamp": "2025-12-05T11:19:20Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/94c833a21232a31c73e976fd0916e156e8add1e6"
        },
        "date": 1765181814720,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 131.54783824555554,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 425.9654036805555,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 171.94252191524927,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 545.463662631746,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}