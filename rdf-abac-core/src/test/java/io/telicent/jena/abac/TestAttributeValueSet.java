package io.telicent.jena.abac;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

public class TestAttributeValueSet {

    @Test
    public void testOfTrue() {
        AttributeValue value = AttributeValue.of("some", ValueTerm.value(true));
        AttributeValueSet set = AttributeValueSet.of(value);
        Attribute attr = new Attribute("some");
        assertTrue(set.hasAttribute(attr));
    }

    @Test
    public void testOfFalse() {
        AttributeValue value = AttributeValue.of("some", ValueTerm.value(true));
        AttributeValueSet set = AttributeValueSet.of(value);
        Attribute attr = new Attribute("other");
        assertFalse(set.hasAttribute(attr));
    }

    @Test
    public void testHasAttributeTrue() {
        AttributeValueSet set = AttributeValueSet.of("test");
        Attribute test = new Attribute("test");
        assertTrue(set.hasAttribute(test));
    }

    @Test
    public void testHasAttributeFalse() {
        AttributeValueSet set = AttributeValueSet.of("some");
        Attribute test = new Attribute("other");
        assertFalse(set.hasAttribute(test));
    }

    @Test
    public void testAsString() {
        AttributeValueSet set = AttributeValueSet.of("some");
        assertEquals("some=true", set.asString());
    }

    @Test
    public void testEqualsTrue() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        AttributeValueSet set2 = AttributeValueSet.of("test");
        assertTrue(set1.equals(set2)); // we are specifically testing the equals method here
    }

    @Test
    public void testEqualsTrueAsSame() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        AttributeValueSet set2 = AttributeValueSet.of("test");
        assertTrue(set1.equals(set1)); // we are specifically testing the equals method here
    }

    @Test
    public void testEqualsFalse() {
        AttributeValueSet set1 = AttributeValueSet.of("some");
        AttributeValueSet set2 = AttributeValueSet.of("other");
        assertFalse(set1.equals(set2)); // we are specifically testing the equals method here
    }

    @Test
    public void testEqualsFalseAsNull() {
        AttributeValueSet set1 = AttributeValueSet.of("some");
        assertFalse(set1.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void testEqualsFalseAsDifferentClass() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        String test = "test";
        assertFalse(set1.equals(test)); // we are specifically testing the equals method here
    }

    @Test
    public void testHashCode() {
        AttributeValueSet set = AttributeValueSet.of("test");
        assertEquals(3595359,set.hashCode());
    }

    @Test
    public void testIsEmptyTrue() {
        AttributeValueSet set = AttributeValueSet.of();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testIsEmptyFalse() {
        AttributeValueSet set = AttributeValueSet.of("test");
        assertFalse(set.isEmpty());
    }

    @Test
    public void testAttributes() {
        AttributeValueSet set = AttributeValueSet.of("some","other");
        Collection<Attribute> attrs = set.attributes();
        assertEquals(2, attrs.size());
        assertTrue(attrs.contains(new Attribute("some")));
        assertTrue(attrs.contains(new Attribute("other")));
    }

}
