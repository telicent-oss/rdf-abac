package io.telicent.jena.abac.attributes.syntax;

import io.telicent.jena.abac.attributes.AttributeException;
import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TestAE_AttrValue {

    @Test
    void test_is_string_true() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertTrue(value.isString());
    }

    @Test
    void test_is_string_false() {
        AE_AttrValue value = AE_AttrValue.create("true");
        assertFalse(value.isString());
    }

    @Test
    void test_get_string() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertEquals("a", value.getString());
    }

    @Test
    void test_get_string_exception() {
        AE_AttrValue value = AE_AttrValue.create("true");
        Exception exception = assertThrows(AttributeException.class, value::getString);
        assertEquals("Not a string value", exception.getMessage());
    }

    @Test
    void test_get_boolean_true() {
        AE_AttrValue value = AE_AttrValue.create("true");
        assertEquals(true, value.getBoolean());
    }

    @Test
    void test_get_boolean_false() {
        AE_AttrValue value = AE_AttrValue.create("false");
        assertEquals(false, value.getBoolean());
    }

    @Test
    void test_get_boolean_exception() {
        AE_AttrValue value = AE_AttrValue.create("a");
        Exception exception = assertThrows(AttributeException.class, value::getBoolean);
        assertEquals("Not a boolean value", exception.getMessage());
    }

    @Test
    void test_to_string() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertEquals("a", value.toString());
    }

    @Test
    void test_to_string_boolean_true() {
        AE_AttrValue value = AE_AttrValue.create("true");
        assertEquals("true", value.toString());
    }

    @Test
    void test_to_string_boolean_false() {
        AE_AttrValue value = AE_AttrValue.create("false");
        assertEquals("false", value.toString());
    }

    @Test
    void test_print_boolean_true() {
        AE_AttrValue value = AE_AttrValue.create("true");
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        value.print(mockWriter);
        verify(mockWriter).print("true");
    }

    @Test
    void test_print_boolean_false() {
        AE_AttrValue value = AE_AttrValue.create("false");
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        value.print(mockWriter);
        verify(mockWriter).print("false");
    }

    @Test
    void test_hash_code() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertEquals(value.hashCode(),value.hashCode());
    }

    @Test
    void test_equals_same() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertTrue(value.equals(value)); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_null() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertFalse(value.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_different_class() {
        AE_AttrValue value = AE_AttrValue.create("a");
        assertFalse(value.equals("a")); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_similar_01() {
        AE_AttrValue value1 = AE_AttrValue.create("true");
        AE_AttrValue value2 = AE_AttrValue.create("true");
        assertTrue(value1.equals(value2)); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_similar_02() {
        AE_AttrValue value1 = AE_AttrValue.create("a");
        AE_AttrValue value2 = AE_AttrValue.create("a");
        assertTrue(value1.equals(value2)); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_different_01() {
        AE_AttrValue value1 = AE_AttrValue.create("a");
        AE_AttrValue value2 = AE_AttrValue.create("b");
        assertFalse(value1.equals(value2)); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_different_02() {
        AE_AttrValue value1 = AE_AttrValue.create("true");
        AE_AttrValue value2 = AE_AttrValue.create("false");
        assertFalse(value1.equals(value2)); // we are specifically testing the equals method here
    }
}
