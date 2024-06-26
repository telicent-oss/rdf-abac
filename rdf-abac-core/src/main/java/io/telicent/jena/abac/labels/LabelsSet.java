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

import io.telicent.jena.abac.core.Decision;

/**
 * The usage interface to labels.
 */
public class LabelsSet {

    /** If there are no labels in the labels set (labels.get(triple) == null). */
    private static final Decision DefaultChoiceNoLabels = Decision.ALLOW;

    /** If there is an empty set of labels for a triple (labels.get(triple) == {}) */
    private static final Decision DefaultChoiceNoAttributesForTriple = Decision.ALLOW;

    /** LabelsSet wide default. e.g. {@link Decision.NONE} */
    private static final Decision DefaultLabelsSet = Decision.ALLOW;
}
