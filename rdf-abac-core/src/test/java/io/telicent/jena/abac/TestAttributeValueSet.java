package io.telicent.jena.abac;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class TestAttributeValueSet {

    @Test
    void testOfTrue() {
        AttributeValue value = AttributeValue.of("some", ValueTerm.value(true));
        AttributeValueSet set = AttributeValueSet.of(value);
        Attribute attr = new Attribute("some");
        assertTrue(set.hasAttribute(attr));
    }

    @Test
    void testOfFalse() {
        AttributeValue value = AttributeValue.of("some", ValueTerm.value(true));
        AttributeValueSet set = AttributeValueSet.of(value);
        Attribute attr = new Attribute("other");
        assertFalse(set.hasAttribute(attr));
    }

    @Test
    void testHasAttributeTrue() {
        AttributeValueSet set = AttributeValueSet.of("test");
        Attribute test = new Attribute("test");
        assertTrue(set.hasAttribute(test));
    }

    @Test
    void testHasAttributeFalse() {
        AttributeValueSet set = AttributeValueSet.of("some");
        Attribute test = new Attribute("other");
        assertFalse(set.hasAttribute(test));
    }

    @Test
    void testAsString() {
        AttributeValueSet set = AttributeValueSet.of("some");
        assertEquals("some=true", set.asString());
    }

    @Test
    void testEqualsTrue() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        AttributeValueSet set2 = AttributeValueSet.of("test");
        assertTrue(set1.equals(set2)); // we are specifically testing the equals method here
    }

    @Test
    void testEqualsTrueAsSame() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        assertTrue(set1.equals(set1)); // we are specifically testing the equals method here
    }

    @Test
    void testEqualsFalse() {
        AttributeValueSet set1 = AttributeValueSet.of("some");
        AttributeValueSet set2 = AttributeValueSet.of("other");
        assertFalse(set1.equals(set2)); // we are specifically testing the equals method here
    }

    @Test
    void testEqualsFalseAsNull() {
        AttributeValueSet set1 = AttributeValueSet.of("some");
        assertFalse(set1.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    void testEqualsFalseAsDifferentClass() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        String test = "test";
        assertFalse(set1.equals(test)); // we are specifically testing the equals method here
    }

    @Test
    void testHashCode() {
        AttributeValueSet set = AttributeValueSet.of("test");
        assertEquals(3595359,set.hashCode());
    }

    @Test
    void testIsEmptyTrue() {
        AttributeValueSet set = AttributeValueSet.of();
        assertTrue(set.isEmpty());
    }

    @Test
    void testIsEmptyFalse() {
        AttributeValueSet set = AttributeValueSet.of("test");
        assertFalse(set.isEmpty());
    }

    @Test
    void testAttributes() {
        AttributeValueSet set = AttributeValueSet.of("some","other");
        Collection<Attribute> attrs = set.attributes();
        assertEquals(2, attrs.size());
        assertTrue(attrs.contains(new Attribute("some")));
        assertTrue(attrs.contains(new Attribute("other")));
    }

}
