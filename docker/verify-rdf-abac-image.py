#
# Copyright (C) Telicent Ltd
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

"""Validate the data-only RDF-ABAC image before publishing it."""

import argparse
import hashlib
import io
import json
from pathlib import Path
import subprocess
import tarfile
import tempfile
import zipfile


SERVICE = "META-INF/services/io.telicent.smart.cache.security.data.plugins.DataSecurityPlugin"
PROVIDER = "io.telicent.smart.cache.security.data.plugins.rdf.abac.RdfAbacPlugin"
# NB - These exclusions must be kept in sync with the copy-plugin-runtime-dependencies execution in
#      data-security-plugin-rdf-abac/pom.xml
HOST_GROUPS = {"io.telicent.smart-caches", "org.slf4j", "org.apache.jena", "org.apache.kafka", "jakarta.servlet"}
HOST_ARTIFACTS = {"jena-kafka-connector", "zstd-jni", "snappy-java", "lz4-java"}
PLUGIN_ARTIFACTS = {"data-security-plugin-rdf-abac", "data-security-plugin-rdf-abac-registration"}
TEST_ARTIFACTS = {"testng", "mockito-core", "lombok", "logback-core", "logback-classic"}


def require(condition, message):
    if not condition:
        raise ValueError(message)


def validate(archive, expected):
    registrations = []
    with tarfile.open(archive) as image:
        manifest = json.load(image.extractfile("manifest.json"))
        require(len(manifest) == 1, "Expected one image")
        config = json.load(image.extractfile(manifest[0]["Config"]))["config"]
        require(not config.get("Entrypoint") and not config.get("Cmd"), "Image must have no entrypoint or command")
        require(len(manifest[0]["Layers"]) == 1, "Expected one scratch image layer")
        with tarfile.open(fileobj=image.extractfile(manifest[0]["Layers"][0])) as layer:
            members = layer.getmembers()
            require(len(members) == len(expected) and {m.name for m in members} == set(expected),
                    "Image must contain exactly the expected root JARs")
            for member in members:
                require(member.isfile() and "/" not in member.name and member.mode == 0o444,
                        f"Not a read-only root JAR: {member.name}")
                data = layer.extractfile(member).read()
                require(hashlib.sha256(data).digest() == hashlib.sha256(expected[member.name].read_bytes()).digest(),
                        f"Image JAR differs from Maven artifact: {member.name}")
                with zipfile.ZipFile(io.BytesIO(data)) as jar:
                    if SERVICE in jar.namelist():
                        registrations.extend(line.split("#", 1)[0].strip() for line in
                                             jar.read(SERVICE).decode().splitlines() if line.split("#", 1)[0].strip())
                    for name in jar.namelist():
                        if name.startswith("META-INF/maven/") and name.endswith("/pom.properties"):
                            props = dict(line.split("=", 1) for line in jar.read(name).decode().splitlines()
                                         if "=" in line and not line.startswith("#"))
                            group, artifact = props.get("groupId"), props.get("artifactId")
                            require(artifact not in HOST_ARTIFACTS | TEST_ARTIFACTS,
                                    f"Host/test dependency in image: {group}:{artifact}")
                            require(group not in HOST_GROUPS or
                                    (group == "io.telicent.smart-caches" and artifact in PLUGIN_ARTIFACTS),
                                    f"Host dependency in image: {group}:{artifact}")
    require(registrations == [PROVIDER], f"Expected exactly one RDF-ABAC service registration: {registrations}")
    print(f"Validated image: {len(expected)} read-only root JARs and exactly one RDF-ABAC registration")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("image")
    parser.add_argument("version")
    args = parser.parse_args()
    root = Path(__file__).resolve().parent.parent
    impl = root / "data-security-plugin-rdf-abac/target"
    registration = root / "data-security-plugin-rdf-abac-registration/target"
    jars = list((impl / "lib").glob("*.jar"))
    require(jars, "Build the plugin runtime dependencies first")
    jars += [impl / f"data-security-plugin-rdf-abac-{args.version}.jar",
             registration / f"data-security-plugin-rdf-abac-registration-{args.version}.jar"]
    expected = {jar.name: jar for jar in jars}
    require(len(expected) == len(jars), "Duplicate JAR names")
    with tempfile.TemporaryDirectory(prefix="rdf-abac-image-") as temp:
        archive = Path(temp) / "image.tar"
        subprocess.run(["docker", "image", "save", "-o", str(archive), args.image], check=True)
        validate(archive, expected)


if __name__ == "__main__":
    main()
