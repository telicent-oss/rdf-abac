package io.telicent.jena.abac.core;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.apache.jena.atlas.lib.Cache;
import org.apache.jena.sparql.core.DatasetGraph;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@SuppressWarnings("java:S5786")
public class TestCtxABAC {

    @Test
    void evaluation_results_are_isolated_between_contexts() {
        CxtABAC first = CxtABAC.context(AttributeValueSet.EMPTY, mock(HierarchyGetter.class), null);
        CxtABAC second = CxtABAC.context(AttributeValueSet.EMPTY, mock(HierarchyGetter.class), null);
        var label = io.telicent.jena.abac.labels.Label.fromText("true");
        assertSame(ValueTerm.TRUE, first.labelEvalCache().get(label, ignored -> ValueTerm.TRUE));
        assertSame(ValueTerm.FALSE, second.labelEvalCache().get(label, ignored -> ValueTerm.FALSE));
        assertSame(ValueTerm.TRUE, first.labelEvalCache().get(label, ignored -> fail("Expected cached result")));
    }

    @Test
    void evaluation_cache_is_bounded() {
        Cache<Integer, ValueTerm> cache = CxtABAC.createEvalCache(4);
        for (int i = 0; i < 100; i++) {
            cache.put(i, ValueTerm.TRUE);
        }
        ((org.apache.jena.atlas.lib.cache.CacheCaffeine<Integer, ValueTerm>) cache).cleanUp();
        assertTrue(cache.size() <= 4);
        assertFalse(cache.isEmpty());
        cache.clear();
        assertTrue(cache.isEmpty());
    }

    @Test
    void evaluation_cache_does_not_retain_failed_evaluations() {
        Cache<String, ValueTerm> cache = CxtABAC.createEvalCache(4);
        assertThrows(IllegalStateException.class, () -> cache.get("label", ignored -> {
            throw new IllegalStateException("Failed evaluation");
        }));
        assertNull(cache.getIfPresent("label"));
        assertNull(cache.get("label", ignored -> null));
        assertSame(ValueTerm.TRUE, cache.get("label", ignored -> ValueTerm.TRUE));
    }

    @Test
    void test_create_eval_cache_zero() {
        Cache<String, ValueTerm> cache = CxtABAC.createEvalCache(0);
        assertEquals(0, cache.size());
    }

    @Test
    void test_create_hierarchy_cache_zero() {
        Cache<Attribute, Optional<Hierarchy>> cache = CxtABAC.createHierarchyCache(0);
        assertEquals(0, cache.size());
    }

    @Test
    void test_data() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        assertNotNull(cxtABAC.data());
    }

    @Test
    void test_request_id() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        assertNotNull(cxtABAC.requestId());
    }

    @Test
    void test_tracking_none() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.NONE);
        assertEquals(Track.NONE, cxtABAC.tracking());
    }

    @Test
    void test_tracking_debug() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.DEBUG);
        assertEquals(Track.DEBUG, cxtABAC.tracking());
    }

    @Test
    void test_tracking_trace() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.TRACE);
        assertEquals(Track.TRACE, cxtABAC.tracking());
    }

    @Test
    void test_debug_true() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.DEBUG);
        assertTrue(cxtABAC.debug());
    }

    @Test
    void test_debug_true_with_trace() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.TRACE);
        assertTrue(cxtABAC.debug());
    }

    @Test
    void test_debug_false() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.NONE);
        assertFalse(cxtABAC.debug());
    }

    @Test
    void test_trace_true() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.TRACE);
        assertTrue(cxtABAC.trace());
    }

    @Test
    void test_trace_false() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        cxtABAC.tracking(Track.NONE);
        assertFalse(cxtABAC.trace());
    }

    @Test
    void test_system_trace() {
        AttributeValueSet mockAttributeValueSet = mock(AttributeValueSet.class);
        HierarchyGetter mockHierarchyGetter = mock(HierarchyGetter.class);
        DatasetGraph mockDatasetGraph = mock(DatasetGraph.class);
        CxtABAC.systemTrace(Track.NONE);
        CxtABAC cxtABAC = CxtABAC.context(mockAttributeValueSet,mockHierarchyGetter,mockDatasetGraph);
        assertEquals(Track.NONE, cxtABAC.tracking());
    }
}
