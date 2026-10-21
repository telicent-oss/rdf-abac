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

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.security.data.labels.MalformedLabelsException;
import io.telicent.smart.cache.security.data.labels.SecurityLabels;
import io.telicent.smart.cache.security.data.labels.SecurityLabelsApplicator;
import io.telicent.smart.cache.security.data.plugins.AbstractDataSecurityPluginTests;
import org.apache.jena.sparql.core.Quad;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

@SuppressWarnings("resource")
public class TestRdfAbacApplicator {

    protected static final RdfAbacParser PARSER = new RdfAbacParser();

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullParser_whenConstructing_thenNullPointerException() {
        new RdfAbacApplicator(null, null, mock(LabelsStore.class), false);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullLabelsStore_whenConstructing_thenNullPointerException() {
        new RdfAbacApplicator(PARSER, null, null, false);
    }

    @Test
    public void givenLabelsStoreWithLabel_whenApplyingToTriple_thenEncodedMatchesLabelData() {
        // Given
        final Label label = Label.fromText("clearance=S");
        final LabelsStore store = mock(LabelsStore.class);
        when(store.labelForQuad(any())).thenReturn(label);

        // When
        try (final RdfAbacApplicator applicator = new RdfAbacApplicator(PARSER, null, store, false)) {
            final SecurityLabels<?> applied = applicator.labelForTriple(AbstractDataSecurityPluginTests.TEST_TRIPLE);

            // Then
            Assert.assertNotNull(applied);
            Assert.assertEquals(applied.encoded(), label.data());
        }
    }

    @Test
    public void givenLabelsStoreWithLabel_whenApplyingToQuad_thenEncodedMatchesLabelData() {
        // Given
        final Label label = Label.fromText("clearance=S");
        final LabelsStore store = mock(LabelsStore.class);
        when(store.labelForQuad(any())).thenReturn(label);
        final Quad quad = Quad.create(Quad.defaultGraphIRI, AbstractDataSecurityPluginTests.TEST_TRIPLE);

        // When
        try (final RdfAbacApplicator applicator = new RdfAbacApplicator(PARSER, null, store, false)) {
            final SecurityLabels<?> applied = applicator.labelForQuad(quad);

            // Then
            Assert.assertNotNull(applied);
            Assert.assertEquals(applied.encoded(), label.data());
        }
    }

    @Test
    public void givenLabelsStore_whenObtainedDefaultLabel_thenDefaultAsExpected() {
        // Given
        SecurityLabels<?> defaultLabel = mock(SecurityLabels.class);
        final LabelsStore store = mock(LabelsStore.class);

        // When
        try (final RdfAbacApplicator applicator = new RdfAbacApplicator(PARSER, defaultLabel, store, false)) {
            final SecurityLabels<?> applied = applicator.defaultLabel();

            // Then
            Assert.assertNotNull(applied);
            Assert.assertSame(applied, defaultLabel);
        }
    }

    @Test(expectedExceptions = MalformedLabelsException.class)
    public void givenLabelsStoreWithInvalidLabel_whenApplyingToTriple_thenMalformedLabelsException() {
        // Given
        final Label label = new Label("clearance=".getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
        final LabelsStore store = mock(LabelsStore.class);
        when(store.labelForQuad(any())).thenReturn(label);

        // When
        try (final RdfAbacApplicator applicator = new RdfAbacApplicator(PARSER, null, store, false)) {
            applicator.labelForTriple(AbstractDataSecurityPluginTests.TEST_TRIPLE);
        }
    }

    @Test
    public void givenLabelsStore_whenClosing_thenStoreIsNotClosed() throws Exception {
        // Given
        final LabelsStore store = mock(LabelsStore.class);

        // When
        new RdfAbacApplicator(PARSER, null, store, false).close();

        // Then - LabelsStore is owned by DatasetGraphABAC so the applicator must not close it
        verify(store, never()).close();
    }

    @Test
    public void givenNonOwnedLabelsStore_whenClosing_thenLabelsStoreNotClosed() throws Exception {
        // Given
        final LabelsStore store = mock(LabelsStore.class);

        // When
        try (SecurityLabelsApplicator applicator = new RdfAbacApplicator(PARSER, null, store, false)) {
            final SecurityLabels<?> applied = applicator.labelForTriple(AbstractDataSecurityPluginTests.TEST_TRIPLE);
            Assert.assertNull(applied);
        }

        // Then
        verify(store, never()).close();
    }

    @Test
    public void givenOwnedLabelsStore_whenClosing_thenLabelsStoreNotClosed() throws Exception {
        // Given
        final LabelsStore store = mock(LabelsStore.class);

        // When
        try (SecurityLabelsApplicator applicator = new RdfAbacApplicator(PARSER, null, store, true)) {
            final SecurityLabels<?> applied = applicator.labelForTriple(AbstractDataSecurityPluginTests.TEST_TRIPLE);
            Assert.assertNull(applied);
        }

        // Then
        verify(store, times(1)).close();
    }

    @Test
    public void givenFailsOnCloseLabelsStore_whenClosing_thenNoError() throws Exception {
        // Given
        final Label label = Label.fromText("clearance=S");
        final LabelsStore store = mock(LabelsStore.class);
        when(store.labelForQuad(any())).thenReturn(label);
        doThrow(new RuntimeException("failed")).when(store).close();

        // When / Then - close exception is swallowed, no error propagated
        try (final RdfAbacApplicator applicator = new RdfAbacApplicator(PARSER, null, store, true)) {
            final SecurityLabels<?> applied = applicator.labelForTriple(AbstractDataSecurityPluginTests.TEST_TRIPLE);
            Assert.assertNotNull(applied);
        }
        verify(store, times(1)).close();
    }

}
