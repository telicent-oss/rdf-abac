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

@SuppressWarnings("java:S1481")
class TestValueTerm {

    private IndentedWriter writer;

    @BeforeEach
    void setUp() {
        writer = mock(IndentedWriter.class);
    }

    @Test
    void testIsString01() {
        ValueTerm v1 = value("abc");
        assertTrue(v1.isString());
    }

    @Test
    void testIsString02() {
        ValueTerm v1 = value(true);
        assertFalse(v1.isString());
    }

    @Test
    void testGetString01() {
        ValueTerm v1 = value("abc");
        String v1String = v1.getString();
        assertEquals("abc", v1String);
    }

    @Test
    void testGetString02() {
        ValueTerm value = value(false);
        Exception exception = assertThrows(AttributeException.class, value::getString);
        assertEquals("Not a string value", exception.getMessage());
    }

    @Test
    void testGetBoolean01() {
        ValueTerm v1 = value(false);
        Boolean v1Boolean = v1.getBoolean();
        assertFalse(v1Boolean);
    }

    @Test
    void testGetBoolean02() {
        ValueTerm value = value("abc");
        Exception exception = assertThrows(AttributeException.class, value::getBoolean);
        assertEquals("Not a boolean value", exception.getMessage());
    }

    @Test
    void testAsString01() {
        ValueTerm v1 = value(true);
        String v1String = v1.asString();
        assertEquals("true", v1String);
    }

    @Test
    void testAsString02() {
        ValueTerm v1 = value(false);
        String v1String = v1.asString();
        assertEquals("false", v1String);
    }

    @Test
    void testAsString03() {
        ValueTerm v1 = value("abc");
        String v1String = v1.asString();
        assertEquals("abc", v1String);
    }

    @Test
    void testPrint01() {
        ValueTerm v1 = value(true);
        v1.print(writer);
        verify(writer).print("true");
        verifyNoMoreInteractions(writer); //
    }

    @Test
    void testPrint02() {
        ValueTerm v1 = value(false);
        v1.print(writer);
        verify(writer).print("false");
        verifyNoMoreInteractions(writer); //
    }

    @Test
    void testPrint03() {
        ValueTerm v1 = value("hehe");
        v1.print(writer);
        try (MockedStatic<Words> mockedWords = mockStatic(Words.class)) {
            v1.print(writer);
            mockedWords.verify(() -> Words.print(writer, "hehe"));
        }
    }

    @Test
    void testEquals01() {
        ValueTerm v1 = value("abc");
        ValueTerm v2 = value("abc");
        assertEquals(v1, v2);
    }

    @Test
    void testEquals02() {
        ValueTerm v1 = value("abc");
        assertEquals(v1, v1);
    }

    @Test
    void testEquals03() {
        AttributeValue av1 = AE.parseAttrValue("k=v");
        ValueTerm v2 = ValueTerm.value(true);
        assertNotEquals(v2, av1);
    }

    @Test
    void testEquals04() {
        ValueTerm v1 = value(true);
        ValueTerm v2 = value("a");
        assertNotEquals(v1, v2);
    }

    @Test
    void testEquals05() {
        ValueTerm v1 = value("abc");
        assertNotEquals(v1, null);
    }
}
