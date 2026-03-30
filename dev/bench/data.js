window.BENCHMARK_DATA = {
  "lastUpdate": 1774878588585,
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
          "id": "178a408c30da087748aa2092025c92965578c1af",
          "message": "Merge pull request #174 from telicent-oss/dependabot/maven/dependency.jena-6.0.0\n\nBump dependency.jena from 5.6.0 to 6.0.0",
          "timestamp": "2026-02-10T09:07:54Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/178a408c30da087748aa2092025c92965578c1af"
        },
        "date": 1771230330875,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 109.62300714949212,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 403.16939724,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 151.45858805356588,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 492.9018885583262,
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
          "id": "9da6e403a21864a22925e451e6a4dad004ffb59c",
          "message": "Merge pull request #177 from telicent-oss/dependabot/maven/patches-190f324d1e\n\nBump org.apache.maven.plugins:maven-surefire-plugin from 3.5.4 to 3.5.5 in the patches group",
          "timestamp": "2026-02-23T07:57:09Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/9da6e403a21864a22925e451e6a4dad004ffb59c"
        },
        "date": 1771835172601,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 116.60309029457365,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 416.6474500261111,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 155.53692704615386,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 564.4377182301588,
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
          "id": "df8cdd0f9e03c48ec46b46bde1845d9d74daab3a",
          "message": "Merge pull request #181 from telicent-oss/release/2.0.0\n\nComplete 2.0.0 Release",
          "timestamp": "2026-02-25T14:29:27Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/df8cdd0f9e03c48ec46b46bde1845d9d74daab3a"
        },
        "date": 1772439887129,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 153.21902982174646,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 429.47638149891304,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 161.65878252184675,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 600.4812387474359,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          }
        ]
      },
      {
        "commit": {
          "author": {
            "name": "Rob Walpole",
            "username": "robwtelicent",
            "email": "183595007+robwtelicent@users.noreply.github.com"
          },
          "committer": {
            "name": "GitHub",
            "username": "web-flow",
            "email": "noreply@github.com"
          },
          "id": "b0b5f6a682423c4ebfdc725e5077ef5da1442a65",
          "message": "Merge pull request #182 from telicent-oss/dependabot/maven/patches-6a0d3ee640\n\nBump the patches group with 2 updates",
          "timestamp": "2026-03-02T08:58:37Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/b0b5f6a682423c4ebfdc725e5077ef5da1442a65"
        },
        "date": 1773044728090,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 121.64301364056934,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 401.3511781712821,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 164.35665050158647,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 507.1713990908816,
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
          "id": "3f30c6e1f5296c4233f420906fa099455731f233",
          "message": "Merge pull request #184 from telicent-oss/dependabot/maven/patches-f0ac80ae3e\n\nBump the patches group with 2 updates",
          "timestamp": "2026-03-09T11:31:23Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/3f30c6e1f5296c4233f420906fa099455731f233"
        },
        "date": 1773650160581,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 136.35198811368628,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 435.42161634842995,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 167.71562319807046,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 604.5444829862156,
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
          "id": "544143ec67413281a121f922219997f11fabdf9b",
          "message": "Merge pull request #192 from telicent-oss/release/2.0.3\n\nComplete Release 2.0.3",
          "timestamp": "2026-03-20T11:59:10Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/544143ec67413281a121f922219997f11fabdf9b"
        },
        "date": 1774254698413,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 175.15389288884708,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 563.7415062037037,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 237.6907719736065,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 683.0312598769063,
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
          "id": "6112cdcd044820f58afd5e96754ef48757fc94fc",
          "message": "Merge pull request #193 from telicent-oss/dependabot/maven/patches-dcac802b22\n\nBump the patches group with 3 updates",
          "timestamp": "2026-03-30T06:56:30Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/6112cdcd044820f58afd5e96754ef48757fc94fc"
        },
        "date": 1774860319157,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 94.62524427378291,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 493.652079,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 669.6576754222223,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 2105.2046014666666,
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
          "id": "6112cdcd044820f58afd5e96754ef48757fc94fc",
          "message": "Merge pull request #193 from telicent-oss/dependabot/maven/patches-dcac802b22\n\nBump the patches group with 3 updates",
          "timestamp": "2026-03-30T06:56:30Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/6112cdcd044820f58afd5e96754ef48757fc94fc"
        },
        "date": 1774861677040,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 101.04947995325227,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 495.729616,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 816.385760948718,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 2021.3313483333334,
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
          "id": "ec3fde9a1e61febd7f908d95501bb7ca009ff653",
          "message": "Fix typos/grammar errors in docs (CORE-1149)\n\nCo-authored-by: Rob Vesse <rvesse@dotnetrdf.org>",
          "timestamp": "2026-03-30T12:44:35Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/ec3fde9a1e61febd7f908d95501bb7ca009ff653"
        },
        "date": 1774874946159,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 87.02985520091158,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 492.14005574603175,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 148.0816246420384,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 580.6893994190476,
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
            "name": "Rob Vesse",
            "username": "rvesse",
            "email": "rob.vesse@telicent.io"
          },
          "id": "41bfcf07068ab47201c8e9be75efea7705b7915d",
          "message": "Benchmarking improvements (CORE-1149)\n\n- Use more than one warmup iteration to get more stable cache behaviour\n  for read_hot_mixed benchmark\n- Use Maven cache to speed up builds in benchmark jobs",
          "timestamp": "2026-03-30T13:18:54Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/41bfcf07068ab47201c8e9be75efea7705b7915d"
        },
        "date": 1774877067612,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 111.59904716329487,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 507.08370190000005,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 176.23590755789633,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 551.939201,
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
          "id": "7482e67b8a839b95eaa17583053da0add2067651",
          "message": "Merge branch 'main' into modern-rocksdb-store",
          "timestamp": "2026-03-30T13:44:34Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/7482e67b8a839b95eaa17583053da0add2067651"
        },
        "date": 1774878587669,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 102.2575706262171,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_hits ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 488.17392458095236,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"100000\"} )",
            "value": 211.03282204792663,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreRocksDBBaselineBenchmark.read_hot_mixed ( {\"readsPerInvocation\":\"1000000\",\"tripleCount\":\"1000000\"} )",
            "value": 547.5115759298246,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          }
        ]
      }
    ],
    "LabelsStoreModernRocksDBBaselineBenchmark": [
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
          "id": "ec3fde9a1e61febd7f908d95501bb7ca009ff653",
          "message": "Fix typos/grammar errors in docs (CORE-1149)\n\nCo-authored-by: Rob Vesse <rvesse@dotnetrdf.org>",
          "timestamp": "2026-03-30T12:44:35Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/ec3fde9a1e61febd7f908d95501bb7ca009ff653"
        },
        "date": 1774875260457,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreModernRocksDBBaselineBenchmark.read_hot_hits ( {\"quadCount\":\"100000\",\"readsPerInvocation\":\"1000000\"} )",
            "value": 103.95863178118907,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreModernRocksDBBaselineBenchmark.read_hot_hits ( {\"quadCount\":\"1000000\",\"readsPerInvocation\":\"1000000\"} )",
            "value": 386.0777335522317,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreModernRocksDBBaselineBenchmark.read_hot_mixed ( {\"quadCount\":\"100000\",\"readsPerInvocation\":\"1000000\"} )",
            "value": 120.75462851405622,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreModernRocksDBBaselineBenchmark.read_hot_mixed ( {\"quadCount\":\"1000000\",\"readsPerInvocation\":\"1000000\"} )",
            "value": 801.6788940662526,
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
            "name": "Rob Vesse",
            "username": "rvesse",
            "email": "rob.vesse@telicent.io"
          },
          "id": "5114b1340d14d8a71d7c0558dc983529253aba73",
          "message": "Fix duplicated dependency definition",
          "timestamp": "2026-03-30T13:26:13Z",
          "url": "https://github.com/telicent-oss/rdf-abac/commit/5114b1340d14d8a71d7c0558dc983529253aba73"
        },
        "date": 1774877415669,
        "tool": "jmh",
        "benches": [
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreModernRocksDBBaselineBenchmark.read_hot_hits ( {\"quadCount\":\"100000\",\"readsPerInvocation\":\"1000000\"} )",
            "value": 94.0369891750975,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreModernRocksDBBaselineBenchmark.read_hot_hits ( {\"quadCount\":\"1000000\",\"readsPerInvocation\":\"1000000\"} )",
            "value": 407.53430573692305,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreModernRocksDBBaselineBenchmark.read_hot_mixed ( {\"quadCount\":\"100000\",\"readsPerInvocation\":\"1000000\"} )",
            "value": 136.76367132805913,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          },
          {
            "name": "io.telicent.jena.abac.labels.LabelsStoreModernRocksDBBaselineBenchmark.read_hot_mixed ( {\"quadCount\":\"1000000\",\"readsPerInvocation\":\"1000000\"} )",
            "value": 473.13740431962486,
            "unit": "ms/op",
            "extra": "iterations: 3\nforks: 1\nthreads: 1"
          }
        ]
      }
    ]
  }
}