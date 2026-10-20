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

import com.github.benmanes.caffeine.cache.Caffeine;
import io.telicent.jena.abac.ABAC;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.attributes.syntax.AE_Allow;
import io.telicent.jena.abac.core.AttributesStoreLocal;
import io.telicent.jena.abac.core.CxtABAC;
import io.telicent.jena.abac.labels.LabelsStore;
import io.telicent.smart.cache.security.data.labels.SecurityLabels;
import org.apache.jena.sparql.core.DatasetGraph;
import org.apache.jena.sparql.core.DatasetGraphFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

// java:S4030 - false positive: the collection is consumed by the Mockito thenAnswer lambda below
@SuppressWarnings("java:S4030")
public class TestRdfAbacAuthorizer {


    private static RdfAbacAuthorizer createAuthorizer(int cacheSize) {
        return new RdfAbacAuthorizer(
                CxtABAC.context(AttributeValueSet.EMPTY, new AttributesStoreLocal(), DatasetGraphFactory.empty()),
                Caffeine.newBuilder().maximumSize(cacheSize).build());
    }

    @Test
    public void givenWrongLabels_whenAuthorizing_thenForbidden() {
        // Given
        SecurityLabels<?> labels = mock(SecurityLabels.class);
        when(labels.decodedLabels()).thenAnswer(invocationOnMock -> new Object());

        // When
        try (RdfAbacAuthorizer authorizer = createAuthorizer(1)) {
            // Then
            Assert.assertFalse(authorizer.canRead(labels));
        }
    }

    @Test
    public void givenInaccessibleLabels_whenAuthorizing_thenForbidden() {
        // Given
        SecurityLabels<?> labels = mock(SecurityLabels.class);
        when(labels.decodedLabels()).thenThrow(new RuntimeException("bad"));

        // When
        try (RdfAbacAuthorizer authorizer = createAuthorizer(1)) {
            // Then
            Assert.assertFalse(authorizer.canRead(labels));
        }
    }

    @Test
    public void givenPartiallyWrongLabels_whenAuthorizing_thenForbidden() {
        // Given
        SecurityLabels<?> labels = mock(SecurityLabels.class);
        List<Object> decoded = new ArrayList<>();
        decoded.add(AE_Allow.value());
        decoded.add(null);
        decoded.add(new Object());
        when(labels.decodedLabels()).thenAnswer(invocationOnMock -> decoded);

        // When
        try (RdfAbacAuthorizer authorizer = createAuthorizer(1)) {
            // Then
            Assert.assertFalse(authorizer.canRead(labels));
        }
    }

    @Test
    public void givenNonAbacDataset_whenCheckingSecureDataset_thenFalse() {
        // Given
        DatasetGraph plainDataset = DatasetGraphFactory.createTxnMem();

        // When
        try (RdfAbacAuthorizer authorizer = createAuthorizer(1)) {
            // Then
            Assert.assertFalse(authorizer.isSecureDataset(plainDataset));
        }
    }

    @Test
    public void givenAbacDataset_whenCheckingSecureDataset_thenTrue() {
        // Given
        LabelsStore labelsStore = mock(LabelsStore.class);
        DatasetGraph abacDataset = ABAC.authzDataset(DatasetGraphFactory.createTxnMem(), labelsStore, null, null);

        // When
        try (RdfAbacAuthorizer authorizer = createAuthorizer(1)) {
            // Then
            Assert.assertTrue(authorizer.isSecureDataset(abacDataset));
        }
    }

    private static AttributeExpr mockAttributeExpr() {
        AttributeExpr expr = mock(AttributeExpr.class);
        when(expr.eval(any())).thenReturn(ValueTerm.TRUE);
        return expr;
    }

    @Test
    public void givenLabels_whenAuthorizing_thenDecisionsCached() {
        // Given
        AttributeExpr expr = mockAttributeExpr();
        List<Object> decoded = new ArrayList<>();
        decoded.add(expr);
        SecurityLabels<?> labels = mock(SecurityLabels.class);
        when(labels.decodedLabels()).thenAnswer(invocationOnMock -> decoded);

        // When
        try (RdfAbacAuthorizer authorizer = createAuthorizer(1)) {
            for (int i = 1; i <= 100; i++) {
                Assert.assertTrue(authorizer.canRead(labels));
            }
            verify(expr, times(1)).eval(any());
        }
    }

    @Test
    public void givenManyLabels_whenAuthorizing_thenDecisionsCached() {
        // Given
        List<AttributeExpr> decoded = new ArrayList<>();
        for (int i = 1; i <= 50; i++) {
            AttributeExpr expr = mockAttributeExpr();
            decoded.add(expr);
        }
        SecurityLabels<?> labels = mock(SecurityLabels.class);
        when(labels.decodedLabels()).thenAnswer(invocationOnMock -> decoded);

        // When
        try (RdfAbacAuthorizer authorizer = createAuthorizer(100)) {
            for (int i = 1; i <= 100; i++) {
                Assert.assertTrue(authorizer.canRead(labels));
            }
            for (AttributeExpr expr : decoded) {
                verify(expr, times(1)).eval(any());
            }
        }
    }
}
