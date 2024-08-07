/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.telicent.jena.abac;

import java.util.stream.Stream;

import io.telicent.jena.abac.labels.LabelsStoreMem;
import io.telicent.jena.abac.labels.LabelsStoreMemPattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Run test files on the default in-memory labels store. {@link LabelsStoreMem}
 */
@SuppressWarnings("deprecation")
public class TestLabelsMemPattern extends BaseTestLabels {

    @ParameterizedTest(name = "{0}")
    @MethodSource("labels_files")
    public void labels(String filename, Integer expected) {
        test(filename, expected,  LabelsStoreMemPattern.create());
    }

    protected static Stream<Arguments> labels_files() {
        return BaseTestLabels.labels_files_with_patterns();
    }
}
