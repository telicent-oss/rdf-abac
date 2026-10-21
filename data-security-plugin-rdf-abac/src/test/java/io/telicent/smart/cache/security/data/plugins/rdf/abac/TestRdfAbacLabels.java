/**
 * Copyright (C) Telicent Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.telicent.smart.cache.security.data.plugins.rdf.abac;

import io.telicent.smart.cache.security.data.labels.SecurityLabels;
import org.apache.commons.lang3.Strings;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TestRdfAbacLabels {

    protected static final RdfAbacParser PARSER = new RdfAbacParser();

    @Test
    public void givenLabels_whenComparingForEquality_thenAsExpected() {
        // Given
        SecurityLabels<?> labels = PARSER.parseSecurityLabels("clearance=TS".getBytes(StandardCharsets.UTF_8));
        SecurityLabels<?> otherLabels = PARSER.parseSecurityLabels("clearance=O".getBytes(StandardCharsets.UTF_8));
        Assert.assertTrue(labels instanceof RdfAbacLabels);
        Assert.assertTrue(otherLabels instanceof RdfAbacLabels);

        // When and Then
        Assert.assertTrue(labels.equals(labels));
        Assert.assertFalse(labels.equals(new Object()));
        Assert.assertFalse(labels.equals(null));
        Assert.assertFalse(labels.equals(otherLabels));
    }

    @Test
    public void givenLabels_whenGettingHashCode_thenAsExpected() {
        // Given
        byte[] encoded = "nationality=GBR".getBytes(StandardCharsets.UTF_8);
        SecurityLabels<?> labels = PARSER.parseSecurityLabels(encoded);

        // When and Then
        Assert.assertEquals(labels.hashCode(), Arrays.hashCode(encoded));
    }

    @Test
    public void givenLabels_whenGettingToString_thenUseful() {
        // Given
        byte[] encoded = "nationality=GBR".getBytes(StandardCharsets.UTF_8);
        SecurityLabels<?> labels = PARSER.parseSecurityLabels(encoded);

        // When
        String value = labels.toString();

        // Then
        Assert.assertTrue(Strings.CI.contains(value,"encodedSize=" + encoded.length));
        Assert.assertTrue(Strings.CI.contains(value, "nationality = GBR"));
    }
}
