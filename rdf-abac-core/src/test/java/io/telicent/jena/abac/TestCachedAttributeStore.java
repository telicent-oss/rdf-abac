package io.telicent.jena.abac;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.core.AttributeStoreCache;
import io.telicent.jena.abac.core.AttributesStore;
import io.telicent.jena.abac.core.AttributesStoreModifiable;
import io.telicent.jena.abac.core.DatasetGraphABAC;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.util.Set;

import static io.telicent.jena.abac.TestAssemblerABAC.assemble;
import static io.telicent.jena.abac.TestAssemblerABAC.assembleBad;
import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestCachedAttributeStore {

    private static final String DIR = "src/test/files/dataset/";

    private static final AttributeValueSet SAMPLE_ATTRIBUTE_VALUE_SET =
            AttributeValueSet.of("attribute1=value1,attribute2=value2,attribute3=value3");

    private static final Hierarchy SAMPLE_HIERARCHY =
            Hierarchy.create("Test", "attribute1", "attribute2", "attribute3");


    @Test
    public void test_setup_happyPath() {
        DatasetGraphABAC datasetGraphABAC = assemble(DIR+"abac-assembler-cache-1.ttl");
        assertNotNull(datasetGraphABAC.labelsStore());
        assertNotNull(datasetGraphABAC.attributesStore());
        datasetGraphABAC.close();
    }

    @Test
    public void test_badDurationFormat() {
        assembleBad(DIR+"abac-assembler-cache-bad-1.ttl");
    }

    @Test
    public void test_badDurationFormat2() {
        assembleBad(DIR+"abac-assembler-cache-bad-2.ttl");
    }


    @Test
    public void test_attributes_initialCallMadeToUnderlyingStoreRemainingToCache() {
        //given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        when(mockedStore.attributes("user")).thenReturn(SAMPLE_ATTRIBUTE_VALUE_SET)
                                            .thenThrow(new RuntimeException("Test failed - cache bypassed"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        //when
        AttributeValueSet initialResult = cut.attributes("user");
        AttributeValueSet subsequentResult = cut.attributes("user");
        //then
        assertEquals(initialResult, subsequentResult);
        verify(mockedStore, times(1)).attributes("user");
    }


    @Test
    public void test_getHierarchy_initialCallMadeToUnderlyingStoreRemainingToCache() {
        //given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        Attribute attribute = new Attribute("Test");
        when(mockedStore.getHierarchy(attribute)).thenReturn(SAMPLE_HIERARCHY)
                                                 .thenThrow(new RuntimeException("Test failed - cache bypassed"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        //when
        Hierarchy initialResult = cut.getHierarchy(attribute);
        Hierarchy subsequentResult = cut.getHierarchy(attribute);
        //then
        assertEquals(initialResult, subsequentResult);
        verify(mockedStore, times(1)).getHierarchy(attribute);
    }

    @Test
    public void test_hasHierarchy_initialCallMadeToUnderlyingStoreRemainingToCache() {
        // given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        Attribute attribute = new Attribute("Test");
        when(mockedStore.getHierarchy(attribute)).thenReturn(SAMPLE_HIERARCHY)
                                                 .thenThrow(new RuntimeException("Test failed - cache bypassed"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        // when
        boolean initialResult = cut.hasHierarchy(attribute);
        boolean subsequentResult = cut.hasHierarchy(attribute);
        //then
        assertEquals(initialResult, subsequentResult);
        verify(mockedStore, times(1)).getHierarchy(attribute);
    }

    @Test
    public void test_hasHierarchy_handleNullResult_asFalse() {
        // given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        Attribute attribute = new Attribute("Test");
        when(mockedStore.getHierarchy(attribute)).thenReturn(null);
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        // when
        boolean result = cut.hasHierarchy(attribute);
        //then
        assertFalse(result);
        verify(mockedStore, times(1)).getHierarchy(attribute);
    }

    @Test
    public void test_hasHierarchy_handleEmptyResult_asFalse() {
        // given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        Attribute attribute = new Attribute("Test");
        Hierarchy emptyHierarchy = Hierarchy.create(attribute, emptyList());
        when(mockedStore.getHierarchy(attribute)).thenReturn(emptyHierarchy);
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        // when
        boolean result = cut.hasHierarchy(attribute);
        //then
        assertFalse(result);
        verify(mockedStore, times(1)).getHierarchy(attribute);
    }

    @Test
    public void test_users_callGoesToUnderlyingStoreNotCache() {
        //given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        when(mockedStore.users()).thenReturn(Set.of("user1", "user2"))
                                 .thenReturn(Set.of("user1", "user2", "user3", "user4"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofSeconds(10), Duration.ofSeconds(10));
        //when
        Set<String> initialResult = cut.users();
        Set<String> subsequentResult = cut.users();
        //then
        assertNotEquals(initialResult, subsequentResult);
        verify(mockedStore, times(2)).users();
    }

    @Test
    public void test_attributes_initialCallMadeToUnderlyingStoreAfterCacheExpiry() {
        //given
        AttributesStore mockedStore = Mockito.mock(AttributesStore.class);
        when(mockedStore.attributes("user")).thenReturn(SAMPLE_ATTRIBUTE_VALUE_SET)
                                            .thenReturn(AttributeValueSet.of("attribute1=value1"));
        AttributeStoreCache cut = new AttributeStoreCache(mockedStore, Duration.ofNanos(1), Duration.ofNanos(1));
        //when
        AttributeValueSet initialResult = cut.attributes("user");
        AttributeValueSet subsequentResult = cut.attributes("user");
        //then
        assertNotEquals(initialResult, subsequentResult);
        verify(mockedStore, times(2)).attributes("user");
    }
}