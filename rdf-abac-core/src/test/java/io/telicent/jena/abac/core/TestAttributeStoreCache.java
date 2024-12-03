package io.telicent.jena.abac.core;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestAttributeStoreCache {

    @Test
    public void test_users_true(){
        AttributesStoreLocal asl = new AttributesStoreLocal();
        asl.put("user1", AttributeValueSet.of(AttributeValue.of("k", ValueTerm.TRUE)));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertTrue(cache.users().contains("user1"));
    }

    @Test
    public void test_users_false(){
        AttributesStoreLocal asl = new AttributesStoreLocal();
        asl.put("user1", AttributeValueSet.of(AttributeValue.of("k", ValueTerm.TRUE)));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertFalse(cache.users().contains("user2"));
    }

    @Test
    public void test_has_hierarchy_true() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute attr = new Attribute("attr");
        asl.addHierarchy(new Hierarchy(attr, List.of(ValueTerm.TRUE)));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertTrue(cache.hasHierarchy(attr));
    }

    @Test
    public void test_has_hierarchy_false_01() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute someAttr = new Attribute("some");
        Attribute otherAttr = new Attribute("other");
        asl.addHierarchy(new Hierarchy(someAttr, List.of(ValueTerm.TRUE)));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertFalse(cache.hasHierarchy(otherAttr));
    }

    @Test
    public void test_has_empty_hierarchy() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute attr = new Attribute("attr");
        asl.addHierarchy(new Hierarchy(attr, List.of()));
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertFalse(cache.hasHierarchy(attr));
    }

    @Test
    public void test_attributes() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        AttributeValue av = AttributeValue.of("k",ValueTerm.TRUE);
        AttributeValueSet avs = AttributeValueSet.of(av);
        asl.put("user1", avs);
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertEquals(avs, cache.attributes("user1"));
    }

    @Test
    public void test_get_hierarchy() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute attr = new Attribute("attr");
        Hierarchy hierarchy = new Hierarchy(attr, List.of(ValueTerm.TRUE));
        asl.addHierarchy(hierarchy);
        AttributeStoreCache cache = new AttributeStoreCache(asl, Duration.ofHours(1),Duration.ofHours(1));
        assertEquals(hierarchy, cache.getHierarchy(attr));
    }

}
