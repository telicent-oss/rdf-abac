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

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
cd "$SCRIPT_DIR"
PROJECT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
BRANCH=${BRANCH:-$(git branch --show-current)}
if [ "$BRANCH" = main ]; then
  DEFAULT_TAG=latest
else
  DEFAULT_TAG=${BRANCH:-$PROJECT_VERSION}
fi
TAG=${1:-$DEFAULT_TAG}
TAG=${TAG//\//-}
REGISTRY=${2:-}
IMAGE=telicent-data-security-rdf-abac-plugin
if [ -n "$REGISTRY" ]; then
  IMAGE="${REGISTRY%/}/$IMAGE"
fi

export DOCKER_BUILDKIT=1
ARGS=(--target telicent-data-security-rdf-abac-plugin -f docker/Dockerfile
      --build-arg "PROJECT_VERSION=$PROJECT_VERSION" -t "$IMAGE:$TAG")
if [ -n "$REGISTRY" ] && [ "$BRANCH" = main ] && [ "$TAG" != latest ]; then
  ARGS+=(-t "$IMAGE:latest")
fi
if [ -n "${TARGET_PLATFORMS:-}" ]; then
  ARGS+=(--platform "$TARGET_PLATFORMS")
  if [ -n "$REGISTRY" ]; then
    docker buildx build "${ARGS[@]}" --push .
  else
    # Without a registry, buildx retains a multi-platform result in its local build cache.
    docker buildx build "${ARGS[@]}" .
  fi
else
  docker build "${ARGS[@]}" .
  if [ -n "$REGISTRY" ]; then
    docker push "$IMAGE:$TAG"
    if [ "$BRANCH" = main ] && [ "$TAG" != latest ]; then
      docker push "$IMAGE:latest"
    fi
  fi
fi
