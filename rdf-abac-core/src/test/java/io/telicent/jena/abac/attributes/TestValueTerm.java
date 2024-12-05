package io.telicent.jena.abac.attributes;

import io.telicent.jena.abac.AE;
import io.telicent.jena.abac.attributes.syntax.tokens.Words;
import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static io.telicent.jena.abac.attributes.ValueTerm.value;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TestValueTerm {

    private IndentedWriter writer; // Mocked writer

    @BeforeEach
    void setUp() {
        writer = mock(IndentedWriter.class); // Create a mock of IndentedWriter
    }

    @Test
    public void testIsString01() {
        ValueTerm v1 = value("abc");
        assertTrue(v1.isString());
    }

    @Test
    public void testIsString02() {
        ValueTerm v1 = value(true);
        assertFalse(v1.isString());
    }

    @Test
    public void testGetString01() {
        ValueTerm v1 = value("abc");
        String v1String = v1.getString();
        assertEquals("abc", v1String);
    }

    @Test
    public void testGetString02() {
        Exception exception = assertThrows(AttributeException.class, () -> {
            ValueTerm v1 = value(false);
            String v1String = v1.getString();
        });
        assertEquals("Not a string value", exception.getMessage());
    }

    @Test
    public void testGetBoolean01() {
        ValueTerm v1 = value(false);
        Boolean v1Boolean = v1.getBoolean();
        assertFalse(v1Boolean);
    }

    @Test
    public void testGetBoolean02() {
        Exception exception = assertThrows(AttributeException.class, () -> {
            ValueTerm v1 = value("abc");
            Boolean v1Boolean = v1.getBoolean();
        });
        assertEquals("Not a boolean value", exception.getMessage());
    }

    @Test
    public void testAsString01() {
        ValueTerm v1 = value(true);
        String v1String = v1.asString();
        assertEquals("true", v1String);
    }

    @Test
    public void testAsString02() {
        ValueTerm v1 = value(false);
        String v1String = v1.asString();
        assertEquals("false", v1String);
    }

    @Test
    public void testAsString03() {
        ValueTerm v1 = value("abc");
        String v1String = v1.asString();
        assertEquals("abc", v1String);
    }

    @Test
    public void testPrint01() {
        ValueTerm v1 = value(true);
        v1.print(writer);
        verify(writer).print("true");
        verifyNoMoreInteractions(writer); //
    }

    @Test
    public void testPrint02() {
        ValueTerm v1 = value(false);
        v1.print(writer);
        verify(writer).print("false");
        verifyNoMoreInteractions(writer); //
    }

    @Test
    public void testPrint03() {
        ValueTerm v1 = value("hehe");
        v1.print(writer);
        try (MockedStatic<Words> mockedWords = mockStatic(Words.class)) {
            v1.print(writer);
            mockedWords.verify(() -> Words.print(writer, "hehe"));
        }
    }

    @Test
    public void testEquals01() {
        ValueTerm v1 = value("abc");
        ValueTerm v2 = value("abc");
        assertTrue(v1.equals(v2));
    }

    @Test
    public void testEquals02() {
        ValueTerm v1 = value("abc");
        assertTrue(v1.equals(v1));
    }

    @Test
    public void testEquals03() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        ValueTerm v2 = ValueTerm.value(true);
        assertFalse(v2.equals(av1));
    }

    @Test
    public void testEquals04() {
        ValueTerm v1 = value(true);
        ValueTerm v2 = value("a");
        assertFalse(v1.equals(v2));
    }

    @Test
    public void testEquals05() {
        ValueTerm v1 = value("abc");
        assertFalse(v1.equals(null));
    }
}
