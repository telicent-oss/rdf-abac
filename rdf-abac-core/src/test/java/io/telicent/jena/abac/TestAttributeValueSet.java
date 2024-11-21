package io.telicent.jena.abac;

import io.telicent.jena.abac.attributes.Attribute;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AttributeValueSetTest {

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
        assertEquals(set.asString(),"some=true");
    }

    @Test
    public void testEqualsTrue() {
        AttributeValueSet set1 = AttributeValueSet.of("test");
        AttributeValueSet set2 = AttributeValueSet.of("test");
        assertEquals(set1, set2);
    }

    @Test
    public void testEqualsFalse() {
        AttributeValueSet set1 = AttributeValueSet.of("some");
        AttributeValueSet set2 = AttributeValueSet.of("other");
        assertNotEquals(set1, set2);
    }

    @Test
    public void testHashCode() {
        AttributeValueSet set = AttributeValueSet.of("test");
        assertEquals(3595359,set.hashCode());
    }

}
