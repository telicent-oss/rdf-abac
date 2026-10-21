#!/usr/bin/env bash
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

set -euo pipefail
ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
cd "$ROOT"
PROJECT_VERSION=${1:-$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)}
HOST_VERSION=$(mvn help:evaluate -Dexpression=dependency.smart-caches -q -DforceStdout)
mvn -B -f docker/host/pom.xml clean package -Dhost.core.version="$HOST_VERSION"
PLUGIN_DIR=$(mktemp -d)
trap 'rm -rf "$PLUGIN_DIR"' EXIT
cp "data-security-plugin-rdf-abac/target/data-security-plugin-rdf-abac-$PROJECT_VERSION.jar" "$PLUGIN_DIR/"
cp "data-security-plugin-rdf-abac-registration/target/data-security-plugin-rdf-abac-registration-$PROJECT_VERSION.jar" "$PLUGIN_DIR/"
cp data-security-plugin-rdf-abac/target/lib/*.jar "$PLUGIN_DIR/"
java -cp "$ROOT/docker/host/target/lib/*:$PLUGIN_DIR/*" docker/PluginSmokeTest.java "$PLUGIN_DIR"
