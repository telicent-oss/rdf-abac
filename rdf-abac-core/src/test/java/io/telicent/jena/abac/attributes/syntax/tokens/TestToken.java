package io.telicent.jena.abac.attributes.syntax.tokens;

import org.apache.jena.riot.RiotException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestToken {

    @Test
    public void test_string_type_1() {
        Token token = Token.create("'abc'");
        assertEquals(StringType.STRING1, token.getStringType());
    }

    @Test
    public void test_string_type_2() {
        Token token = Token.create("\"abc\"");
        assertEquals(StringType.STRING2, token.getStringType());
    }

    @Test
    public void test_string_type_long_1() {
        Token token = Token.create("'''abc'''");
        assertEquals(StringType.LONG_STRING1, token.getStringType());
    }

    @Test
    public void test_string_type_long_2() {
        Token token = Token.create("\"\"\"abc\"\"\"");
        assertEquals(StringType.LONG_STRING2, token.getStringType());
    }

    @Test
    public void test_get_column() {
        Token token = Token.create("'abc'");
        assertEquals(1L,token.getColumn());
    }

    @Test
    public void test_get_line() {
        Token token = Token.create("'abc'");
        assertEquals(1L,token.getLine());
    }

    @Test
    public void test_hash_code() {
        Token token = Token.create("'abc'");
        assertEquals(token.hashCode(), token.hashCode());
    }

    @Test
    public void test_equals_same() {
        Token token = Token.create("'abc'");
        assertTrue(token.equals(token)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_null() {
        Token token = Token.create("'abc'");
        assertFalse(token.equals(null)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different_class() {
        Token token = Token.create("'abc'");
        assertFalse(token.equals("'abc'")); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_identical() {
        Token token1 = Token.create("'abc'");
        Token token2 = Token.create("'abc'");
        assertTrue(token1.equals(token2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_equals_different() {
        Token token1 = Token.create("'abc'");
        Token token2 = Token.create("'abb'");
        assertFalse(token1.equals(token2)); // we are specifically testing the equals method here
    }

    @Test
    public void test_create_exception() {
        Exception exception = assertThrows(RiotException.class, () -> {
            Token.create("");
        });
        assertEquals("No token",exception.getMessage());
    }

    @Test
    public void test_as_string() {
        Token token = Token.create("'abc'");
        assertEquals("abc",token.asString());
    }

    @Test
    public void test_as_word() {
        String word = "test";
        Token token = Token.create(word);
        assertEquals(word,token.asWord());
    }

    @Test
    public void test_as_word_null() {
        Token token = Token.create("=");
        assertEquals(null,token.asWord());
    }

    @Test
    public void test_to_string_add_location() {
        Token token = Token.create("'abc'");
        assertEquals("[1,1][STRING:abc]",token.toString(true));
    }

    @Test
    public void test_type_null() {
        Token token = new Token(null,"'abc'");
        token.toString(false);
        assertEquals("[null:'abc']",token.toString(true));
    }

}
