package io.telicent.jena.abac.attributes.syntax;

import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TestAE_Bracketted {

    @Test
    public void test_print() {
        AE_Bracketted brackettedAllow = new AE_Bracketted(AE_Allow.value());
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        brackettedAllow.print(mockWriter);
        verify(mockWriter, times(2)).write(anyString());
    }

    @Test
    public void test_str() {
        AE_Bracketted brackettedAllow = new AE_Bracketted(AE_Allow.value());
        assertEquals("(*)", brackettedAllow.str());
    }

    @Test
    public void test_sym() {
        AE_Bracketted brackettedAllow = new AE_Bracketted(AE_Allow.value());
        assertEquals("[()]",brackettedAllow.sym());
    }

    @Test
    public void test_hash_code() {
        AE_Bracketted brackettedAllow = new AE_Bracketted(AE_Allow.value());
        assertEquals(brackettedAllow.hashCode(), brackettedAllow.hashCode());
    }

    @Test
    public void test_equals_same() {
        AE_Bracketted brackettedAllow = new AE_Bracketted(AE_Allow.value());
        assertTrue(brackettedAllow.equals(brackettedAllow)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_null() {
        AE_Bracketted brackettedAllow = new AE_Bracketted(AE_Allow.value());
        assertFalse(brackettedAllow.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_class() {
        AE_Bracketted brackettedAllow = new AE_Bracketted(AE_Allow.value());
        assertFalse(brackettedAllow.equals("a")); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different() {
        AE_Bracketted brackettedAllow = new AE_Bracketted(AE_Allow.value());
        AE_Bracketted brackettedDeny = new AE_Bracketted(AE_Deny.value());
        assertFalse(brackettedAllow.equals(brackettedDeny)); // we are specifically testing the equals method here
    }
}
