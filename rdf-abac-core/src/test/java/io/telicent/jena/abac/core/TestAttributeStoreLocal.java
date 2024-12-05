package io.telicent.jena.abac.core;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.Hierarchy;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestAttributeStoreLocal {

    @Test
    public void test_users_true() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        asl.put("user1", AttributeValueSet.of(AttributeValue.of("k", ValueTerm.TRUE)));
        assertTrue(asl.users().contains("user1"));
    }

    @Test
    public void test_users_false() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        asl.put("user1", AttributeValueSet.of(AttributeValue.of("k", ValueTerm.TRUE)));
        assertFalse(asl.users().contains("user2"));
    }

    @Test
    public void test_clear() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        asl.put("user1", AttributeValueSet.of(AttributeValue.of("k", ValueTerm.TRUE)));
        assertTrue(asl.users().contains("user1"));
        asl.clear();
        assertTrue(asl.users().isEmpty());
    }

    @Test
    public void test_has_hierarchy_true() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute attr = new Attribute("attr");
        asl.addHierarchy(new Hierarchy(attr, List.of(ValueTerm.TRUE)));
        assertTrue(asl.hasHierarchy(attr));
    }

    @Test
    public void test_has_hierarchy_false() {
        AttributesStoreLocal asl = new AttributesStoreLocal();
        Attribute someAttr = new Attribute("some");
        Attribute otherAttr = new Attribute("other");
        asl.addHierarchy(new Hierarchy(someAttr, List.of(ValueTerm.TRUE)));
        assertFalse(asl.hasHierarchy(otherAttr));
    }


}
