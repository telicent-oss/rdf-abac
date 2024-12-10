package io.telicent.jena.abac.attributes.syntax;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.Operator;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAE_RelAny {

    @Test
    public void test_sym() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals("<", relAny.sym());
    }

    @Test
    public void test_attribute() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals(new Attribute("a"),relAny.attribute());
    }

    @Test
    public void test_attribute_value() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals(ValueTerm.value("1"),relAny.value());
    }

    @Test
    public void test_to_string() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals("(< a 1)",relAny.toString());
    }

    @Test
    public void test_hash_code() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertEquals(relAny.hashCode(),relAny.hashCode());
    }

    @Test
    public void test_equals_same() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertTrue(relAny.equals(relAny)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_null() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertFalse(relAny.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_class() {
        AE_RelAny relAny = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertFalse(relAny.equals("a")); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_identical() {
        AE_RelAny relAny1 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        AE_RelAny relAny2 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertTrue(relAny1.equals(relAny2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_01() {
        AE_RelAny relAny1 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        AE_RelAny relAny2 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("2"));
        assertFalse(relAny1.equals(relAny2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_02() {
        AE_RelAny relAny1 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        AE_RelAny relAny2 = new AE_RelAny(Operator.LT, AE_Attribute.create("b"), AE_AttrValue.create("1"));
        assertFalse(relAny1.equals(relAny2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_03() {
        AE_RelAny relAny1 = new AE_RelAny(Operator.LT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        AE_RelAny relAny2 = new AE_RelAny(Operator.GT, AE_Attribute.create("a"), AE_AttrValue.create("1"));
        assertFalse(relAny1.equals(relAny2)); // we are specifically testing the equals method here
    }
}
