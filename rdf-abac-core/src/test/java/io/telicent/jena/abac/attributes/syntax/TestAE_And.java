package io.telicent.jena.abac.attributes.syntax;

import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@SuppressWarnings("java:S3415")
public class TestAE_And {

    @Test
    void test_sym() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals("&&",aeAnd.sym());
    }

    @Test
    void test_to_string() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals("(&& * {a})", aeAnd.toString());
    }

    @Test
    void test_hash_code() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals(aeAnd.hashCode(),aeAnd.hashCode());
    }

    @Test
    void test_equals_true() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals(aeAnd, new AE_And(AE_Allow.value(), new AE_Var("a"))); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_false_01() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertNotEquals(aeAnd, new AE_And(AE_Allow.value(), new AE_Var("b"))); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_false_02() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertNotEquals(aeAnd, new AE_And(AE_Deny.value(), new AE_Var("a"))); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_false_null() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertNotEquals(aeAnd, null); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_false_class() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertNotEquals("a", aeAnd); // we are specifically testing the equals method here
    }

    @Test
    @SuppressWarnings("java:S3415")
    void test_equals_true_same() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        assertEquals(aeAnd, aeAnd); // we are specifically testing the equals method here
    }

    @Test
    void test_print() {
        AE_And aeAnd = new AE_And(AE_Allow.value(),new AE_Var("a"));
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        aeAnd.print(mockWriter);
        verify(mockWriter, times(4)).write(anyString());
    }

}
