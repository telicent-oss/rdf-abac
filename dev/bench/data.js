window.BENCHMARK_DATA = {
  "lastUpdate": 1770625640667,
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
        "date": 1767601038788,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 118.162194627451,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 408.35648654995725,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 155.23749904259947,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 547.5807140793651,
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
          "id": "5df559508cecb7c451f1a2f137532ba895463b90",
          "message": "Merge pull request #170 from telicent-oss/dependabot/maven/patches-3e05891a12\n\nBump the patches group with 3 updates",
          "timestamp": "2026-01-12T07:47:33Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/5df559508cecb7c451f1a2f137532ba895463b90"
        },
        "date": 1768205819263,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 115.65814732732497,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 394.86063156410256,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 144.8482813488613,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 507.01423464267674,
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
          "id": "5df559508cecb7c451f1a2f137532ba895463b90",
          "message": "Merge pull request #170 from telicent-oss/dependabot/maven/patches-3e05891a12\n\nBump the patches group with 3 updates",
          "timestamp": "2026-01-12T07:47:33Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/5df559508cecb7c451f1a2f137532ba895463b90"
        },
        "date": 1768810675955,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 132.8048850571756,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 441.45964227536234,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 190.4092410898735,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 600.306915214035,
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
          "id": "6ba72d82340f4aae23cda97db2b3816c76657d0f",
          "message": "Merge pull request #171 from telicent-oss/dependabot/maven/patches-099704d0fe\n\nBump org.assertj:assertj-core from 3.27.6 to 3.27.7 in the patches group",
          "timestamp": "2026-01-26T07:47:17Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/6ba72d82340f4aae23cda97db2b3816c76657d0f"
        },
        "date": 1769415434172,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 116.44004650997663,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 410.5106542533333,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 149.04789670851625,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 532.3617428350461,
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
          "id": "41d67ebe6ef49c6ffb5cf6a60c729408b3d15d47",
          "message": "Merge pull request #172 from telicent-oss/dependabot/maven/patches-e466a685ec\n\nBump org.apache.maven.plugins:maven-compiler-plugin from 3.14.1 to 3.15.0 in the patches group",
          "timestamp": "2026-02-02T08:05:30Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/41d67ebe6ef49c6ffb5cf6a60c729408b3d15d47"
        },
        "date": 1770020685053,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 124.02469160642569,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 420.3876067916667,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 165.5034683387978,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 507.03945654619565,
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
          "id": "41d67ebe6ef49c6ffb5cf6a60c729408b3d15d47",
          "message": "Merge pull request #172 from telicent-oss/dependabot/maven/patches-e466a685ec\n\nBump org.apache.maven.plugins:maven-compiler-plugin from 3.14.1 to 3.15.0 in the patches group",
          "timestamp": "2026-02-02T08:05:30Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/41d67ebe6ef49c6ffb5cf6a60c729408b3d15d47"
        },
        "date": 1770625639845,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 114.89554195833334,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 381.99341296296296,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 162.08014015890083,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 493.1919137267976,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}