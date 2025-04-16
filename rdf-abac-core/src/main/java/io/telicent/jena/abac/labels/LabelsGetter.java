/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac.labels;

import java.util.List;
import java.util.function.Function;

import org.apache.jena.graph.Triple;

/**
 * Get labels for a triples.
 * <p>
 * Return an empty list for "no labels found".
 * <p>
 * Return null for "no labels configured" (ABAC not active).
 */
public interface LabelsGetter extends Function<Triple, List<Label>> {}
