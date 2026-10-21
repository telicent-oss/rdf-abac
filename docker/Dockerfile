# syntax=docker/dockerfile:1.7
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

# Data-only image for mounting as a Kubernetes Image Volume. No runtime is needed.
FROM scratch AS telicent-data-security-rdf-abac-plugin

ARG PROJECT_VERSION
COPY --chmod=0444 data-security-plugin-rdf-abac-registration/target/data-security-plugin-rdf-abac-registration-${PROJECT_VERSION}.jar \
     data-security-plugin-rdf-abac/target/data-security-plugin-rdf-abac-${PROJECT_VERSION}.jar \
     data-security-plugin-rdf-abac/target/lib/*.jar \
     /

# Numeric identity works with scratch without adding account files to the JAR-only image.
USER 65532:65532
