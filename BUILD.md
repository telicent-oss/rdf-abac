# Building the RDF-ABAC Plugin Image

The `telicent-data-security-rdf-abac-plugin` image is a data-only image for Kubernetes Image Volumes.
It uses `scratch` and contains only read-only JARs at `/`: the RDF-ABAC implementation, its separate ServiceLoader
registration and non-provided runtime dependencies. It has no entrypoint, command, shell or Java runtime.

Image tags and plugin JAR versions follow this repository's RDF-ABAC version, independently of Smart Caches Core.
The host supplies the `data-security-core` API at the version managed by `dependency.smart-caches` in the root POM.
Use compatible Core, storage, Jena and Fuseki Kafka versions; putting plugin JARs last does not resolve incompatible APIs.

## Build locally

From the repository root, build the plugin and its reactor dependencies:

```bash
mvn -pl data-security-plugin-rdf-abac-registration -am clean package -DskipTests -Dgpg.skip=true
./docker-build.sh local
```

This creates `telicent-data-security-rdf-abac-plugin:local`. CI publishes to Amazon ECR in `eu-west-2`:

```text
<AWS-account>.dkr.ecr.eu-west-2.amazonaws.com/telicent-data-security-rdf-abac-plugin:VERSION
```

For a manual ECR push, authenticate with AWS credentials that can write to that repository:

```bash
ECR_REGISTRY="<AWS-account>.dkr.ecr.eu-west-2.amazonaws.com"
aws ecr get-login-password --region eu-west-2 | docker login --username AWS --password-stdin "$ECR_REGISTRY"
./docker-build.sh VERSION "$ECR_REGISTRY"
```

The optional arguments are `[tag] [registry/namespace]`. Without a tag, the script uses `latest` on `main`, otherwise
the branch name (slashes replaced with hyphens). On `main`, publishing an explicit tag also publishes `latest`.
Set `TARGET_PLATFORMS=linux/amd64,linux/arm64` for a buildx multi-platform build; supply a registry to publish the result.
Without a registry, buildx retains its multi-platform result in the build cache.

## Payload and host dependencies

Maven collects runtime JARs in `data-security-plugin-rdf-abac/target/lib` on every `package`, including the fixed
`package -U -DskipTests` invocation used by the shared publishing action. Use a clean build when dependencies change.
Provided and test dependencies are omitted. Packaging additionally excludes these application-supplied dependencies:

- Groups: `io.telicent.smart-caches`, `org.slf4j`, `org.apache.jena`, `org.apache.kafka`, `jakarta.servlet`.
- Artifacts: `jena-kafka-connector`, `zstd-jni`, `snappy-java`, `lz4-java`.

The implementation and registration JARs retain their `io.telicent.smart-caches` Maven coordinates and are copied
explicitly. Other runtime dependencies, including RDF-ABAC, Fuseki Kafka, storage and RocksDB JNI, remain in the image.
Some supporting libraries can overlap with applications. RocksDB JNI dominates the payload size; the empty base does
not make the JAR payload tiny.

## Verification and publishing

```bash
python3 docker/verify-rdf-abac-image.py telicent-data-security-rdf-abac-plugin:local VERSION
bash docker/smoke-test.sh VERSION
```

The content validator checks the image archive without running it: exact Maven artifact hashes, only read-only root
JARs, exactly one RDF-ABAC service registration, no host/test artifacts, and no entrypoint or command.

The smoke test uses an isolated Maven fixture in `docker/host` to resolve `data-security-core` and its runtime
closure at `dependency.smart-caches`. It does not inherit this repository's dependency management or plugin
implementation. The plugin payload is appended last. The test links plugin classes and exercises authorization,
label application, Fuseki integration and dataset capabilities. This is a baseline host test, not certification of
all application versions or native library paths.

CI runs these checks before the shared `push-container-image` action builds and publishes AMD64 and ARM64 images
to ECR using `registry: aws`. GitHub OIDC (`id-token: write`) assumes `secrets.AWS_ROLE_DEPLOY`; the role must trust
this repository and permit pushing to the `telicent-data-security-rdf-abac-plugin` ECR repository. Provision that
repository before publishing. No Docker Hub publishing credentials are used.

CI tags images with the RDF-ABAC version, a `sha`-prefixed commit hash, and `latest` on `main` or `development` on
other refs. The action includes provenance/SBOM attestations and image signing. Trivy and Grype keep the strict
deployed-image policy, including unfixed high/critical vulnerabilities (`fail-on-unfixed: true`). The shared Trivy
action generates an image-derived CycloneDX SBOM on release tags and uploads it as an artifact and release attachment.
`download-sbom: "false"` disables importing a prebuilt SBOM, not generation of that release SBOM.
Quay publishing and promotion are outside this workflow.
The image itself intentionally contains no SBOM directory, so its root remains JAR-only.

## Deployment follow-up

Consumers should mount the image read-only at `/opt/telicent/security/plugins` and append
`/opt/telicent/security/plugins/*` to the application classpath. A bare directory does not load its JARs.
Application launch-script and Helm chart adoption are separate follow-up tasks; they must validate the host/plugin
version pair and cover both CLI and server launch paths. This change does not deploy or adopt the image in applications.
