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
package io.telicent.smart.cache.security.data.plugins.rdf.abac.utils;

import io.telicent.jena.abac.labels.Label;
import io.telicent.jena.abac.labels.LabelsStore;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.Triple;
import org.apache.jena.sparql.core.Quad;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.*;

public class TestDefaultingLabelsStore {

    private static final byte[] DEFAULT_LABEL = "clearance=S".getBytes(StandardCharsets.UTF_8);
    private static final Label STORED_LABEL = Label.fromText("clearance=TS");
    private static final Triple TEST_TRIPLE = Triple.create(
            NodeFactory.createURI("http://s"),
            NodeFactory.createURI("http://p"),
            NodeFactory.createURI("http://o"));
    private static final Quad TEST_QUAD = Quad.create(Quad.defaultGraphIRI, TEST_TRIPLE);

    @Test(expectedExceptions = NullPointerException.class)
    public void givenNullStore_whenConstructing_thenNullPointerException() {
        new DefaultingLabelsStore(null, DEFAULT_LABEL);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void givenNullDefaultLabel_whenConstructing_thenIllegalArgumentException() {
        new DefaultingLabelsStore(mock(LabelsStore.class), null);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void givenZeroByteDefaultLabel_whenConstructing_thenIllegalArgumentException() {
        new DefaultingLabelsStore(mock(LabelsStore.class), new byte[]{0, 0, 0});
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void givenEmptyDefaultLabel_whenConstructing_thenIllegalArgumentException() {
        new DefaultingLabelsStore(mock(LabelsStore.class), new byte[0]);
    }

    @Test
    public void givenStoreLabelPresent_whenLabelForTriple_thenStoredLabelReturned() {
        final LabelsStore delegate = mock(LabelsStore.class);
        when(delegate.labelForQuad(any())).thenReturn(STORED_LABEL);

        final DefaultingLabelsStore store = new DefaultingLabelsStore(delegate, DEFAULT_LABEL);
        final Label result = store.labelForTriple(TEST_TRIPLE);

        Assert.assertEquals(result, STORED_LABEL);
    }

    @Test
    public void givenNoStoredLabel_whenLabelForTriple_thenDefaultLabelReturned() {
        final LabelsStore delegate = mock(LabelsStore.class);
        when(delegate.labelForQuad(any())).thenReturn(null);

        final DefaultingLabelsStore store = new DefaultingLabelsStore(delegate, DEFAULT_LABEL);
        final Label result = store.labelForTriple(TEST_TRIPLE);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.data(), DEFAULT_LABEL);
    }

    @Test
    public void givenStoreLabelPresent_whenLabelForQuad_thenStoredLabelReturned() {
        final LabelsStore delegate = mock(LabelsStore.class);
        when(delegate.labelForQuad(TEST_QUAD)).thenReturn(STORED_LABEL);

        final DefaultingLabelsStore store = new DefaultingLabelsStore(delegate, DEFAULT_LABEL);
        Label result = store.labelForQuad(TEST_QUAD);

        Assert.assertEquals(result, STORED_LABEL);
    }

    @Test
    public void givenNoStoredLabel_whenLabelForQuad_thenDefaultLabelReturned() {
        final LabelsStore delegate = mock(LabelsStore.class);
        when(delegate.labelForQuad(any())).thenReturn(null);

        final DefaultingLabelsStore store = new DefaultingLabelsStore(delegate, DEFAULT_LABEL);
        final Label result = store.labelForQuad(TEST_QUAD);

        Assert.assertNotNull(result);
        Assert.assertEquals(result.data(), DEFAULT_LABEL);
    }

    @Test
    public void givenTriple_whenLabelForTriple_thenDelegatesUsingDefaultGraph() {
        final LabelsStore delegate = mock(LabelsStore.class);
        when(delegate.labelForQuad(any())).thenReturn(STORED_LABEL);

        final DefaultingLabelsStore store = new DefaultingLabelsStore(delegate, DEFAULT_LABEL);
        store.labelForTriple(TEST_TRIPLE);

        // Triple lookup is done by creating a Quad in the default graph
        verify(delegate).labelForQuad(Quad.create(Quad.defaultGraphIRI, TEST_TRIPLE));
    }
}
