package io.telicent.jena.abac.attributes.syntax;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.core.CxtABAC;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAE_Attribute {

    private final CxtABAC mockContext = mock(CxtABAC.class);

    @Test
    public void test_name() {
        String name = "test";
        AE_Attribute testAttribute = AE_Attribute.create(name);
        assertEquals(name, testAttribute.name());
    }

    @Test
    public void test_eval() {
        Attribute testAttribute = new Attribute("test");
        when(mockContext.getValue(testAttribute)).thenReturn(null);
        assertEquals(ValueTerm.FALSE, AE_Attribute.eval(testAttribute,mockContext));
    }

    @Test
    public void test_equals_same() {
        AE_Attribute testAttribute = AE_Attribute.create("test");
        assertTrue(testAttribute.equals(testAttribute)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_null() {
        AE_Attribute testAttribute = AE_Attribute.create("test");
        assertFalse(testAttribute.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_class() {
        AE_Attribute testAttribute = AE_Attribute.create("test");
        assertFalse(testAttribute.equals("test")); // we are specifically testing the equals method here
    }

}
