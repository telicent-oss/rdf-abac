window.BENCHMARK_DATA = {
  "lastUpdate": 1766996220717,
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
      },
      {
        "commit": {
          "author": {
            "name": "Paul Gallagher",
            "username": "TelicentPaul",
            "email": "132362215+TelicentPaul@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "dc48a845fea9855e878e3db6728c526162398893",
          "message": "Merge pull request #168 from telicent-oss/dependabot/maven/patches-98f23924c1\n\nBump the patches group with 2 updates",
          "timestamp": "2025-12-15T07:55:03Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/dc48a845fea9855e878e3db6728c526162398893"
        },
        "date": 1765786634983,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 91.0671303491883,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 386.50684237986707,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 138.36087363189688,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 544.1850649047619,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Paul Gallagher",
            "username": "TelicentPaul",
            "email": "132362215+TelicentPaul@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "dc48a845fea9855e878e3db6728c526162398893",
          "message": "Merge pull request #168 from telicent-oss/dependabot/maven/patches-98f23924c1\n\nBump the patches group with 2 updates",
          "timestamp": "2025-12-15T07:55:03Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/dc48a845fea9855e878e3db6728c526162398893"
        },
        "date": 1766391388325,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 120.69500068822911,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 417.28207578,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 159.8725017103495,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 534.4134012111111,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Paul Gallagher",
            "username": "TelicentPaul",
            "email": "132362215+TelicentPaul@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "dc48a845fea9855e878e3db6728c526162398893",
          "message": "Merge pull request #168 from telicent-oss/dependabot/maven/patches-98f23924c1\n\nBump the patches group with 2 updates",
          "timestamp": "2025-12-15T07:55:03Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/dc48a845fea9855e878e3db6728c526162398893"
        },
        "date": 1766996220370,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 118.06722264604433,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 406.67282551299144,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 155.5056377656493,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 522.9172602231884,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}