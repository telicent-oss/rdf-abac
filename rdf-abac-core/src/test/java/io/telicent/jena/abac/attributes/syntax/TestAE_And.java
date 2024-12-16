package io.telicent.jena.abac.attributes.syntax;

import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestAE_And {

    @Test
    public void test_sym() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals("&&",aeAnd.sym());
    }

    @Test
    public void test_to_string() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals("(&& * {a})", aeAnd.toString());
    }

    @Test
    public void test_hash_code() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals(aeAnd.hashCode(),aeAnd.hashCode());
    }

    @Test
    public void test_equals_true() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertTrue(aeAnd.equals(new AE_And(AE_Allow.value(), new AE_Var("a")))); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_false_01() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertFalse(aeAnd.equals(new AE_And(AE_Allow.value(), new AE_Var("b")))); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_false_02() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertFalse(aeAnd.equals(new AE_And(AE_Deny.value(), new AE_Var("a")))); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_false_null() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertFalse(aeAnd.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_false_class() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertFalse(aeAnd.equals("a")); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_true_same() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertTrue(aeAnd.equals(aeAnd)); // we are specifically testing the equals method here
    }

    @Test
    public void test_print() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        aeAnd.print(mockWriter);
        verify(mockWriter, times(4)).write(anyString());
    }

}
