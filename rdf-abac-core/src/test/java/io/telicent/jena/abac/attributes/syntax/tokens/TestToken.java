package io.telicent.jena.abac.attributes.syntax.tokens;

import org.apache.jena.riot.RiotException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({ "java:S3415", "java:S5786" })
public class TestToken {

    @Test
    void test_string_type_1() {
        Token token = Token.create("'abc'");
        assertEquals(StringType.STRING1, token.getStringType());
    }

    @Test
    void test_string_type_2() {
        Token token = Token.create("\"abc\"");
        assertEquals(StringType.STRING2, token.getStringType());
    }

    @Test
    void test_string_type_long_1() {
        Token token = Token.create("'''abc'''");
        assertEquals(StringType.LONG_STRING1, token.getStringType());
    }

    @Test
    void test_string_type_long_2() {
        Token token = Token.create("\"\"\"abc\"\"\"");
        assertEquals(StringType.LONG_STRING2, token.getStringType());
    }

    @Test
    void test_get_column() {
        Token token = Token.create("'abc'");
        assertEquals(1L,token.getColumn());
    }

    @Test
    void test_get_line() {
        Token token = Token.create("'abc'");
        assertEquals(1L,token.getLine());
    }

    @Test
    void test_hash_code() {
        Token token = Token.create("'abc'");
        assertEquals(token.hashCode(), token.hashCode());
    }

    @Test
    void test_equals_same() {
        Token token = Token.create("'abc'");
        assertEquals(token, token); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_null() {
        Token token = Token.create("'abc'");
        assertNotEquals(token, null); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_different_class() {
        Token token = Token.create("'abc'");
        assertNotEquals(token, "'abc'"); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_identical() {
        Token token1 = Token.create("'abc'");
        Token token2 = Token.create("'abc'");
        assertEquals(token1, token2); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_different() {
        Token token1 = Token.create("'abc'");
        Token token2 = Token.create("'abb'");
        assertNotEquals(token1, token2); // we are specifically testing the equals method here
    }

    @Test
    void test_create_exception() {
        Exception exception = assertThrows(RiotException.class, () -> Token.create(""));
        assertEquals("No token",exception.getMessage());
    }

    @Test
    void test_as_string() {
        Token token = Token.create("'abc'");
        assertEquals("abc",token.asString());
    }

    @Test
    void test_as_word() {
        String word = "test";
        Token token = Token.create(word);
        assertEquals(word,token.asWord());
    }

    @Test
    void test_as_word_null() {
        Token token = Token.create("=");
        assertEquals(null,token.asWord());
    }

    @Test
    void test_to_string_add_location() {
        Token token = Token.create("'abc'");
        assertEquals("[1,1][STRING:abc]",token.toString(true));
    }

    @Test
    void test_type_null() {
        Token token = new Token(null,"'abc'");
        token.toString(false);
        assertEquals("[null:'abc']",token.toString(true));
    }

}
