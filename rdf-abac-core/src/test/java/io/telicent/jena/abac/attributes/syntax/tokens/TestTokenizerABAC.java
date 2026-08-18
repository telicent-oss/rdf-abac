package io.telicent.jena.abac.attributes.syntax.tokens;

import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({ "java:S4144", "java:S5976", "java:S5786" })
public class TestTokenizerABAC {

    @Test
    void test_next_exception() {
        Tokenizer tokenizer = TokenizerABAC.fromString("");
        assertThrows(NoSuchElementException.class, tokenizer::next);
    }

    @Test
    void test_has_next_broken_long_string() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\"\"");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 4] Broken long string", exception.getMessage());
    }

    @Test
    void test_has_next_long_string_broken_token() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\"\"�");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 5] Broken long string", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "'''a'''", "\"\"\"a\"\"\"", "'''a'b'''", "'''a''b'''", "''a''" })
    void test_has_next_valid_long_strings(String input) {
        Tokenizer tokenizer = TokenizerABAC.fromString(input);
        assertTrue(tokenizer.hasNext());
    }

    @ParameterizedTest
    @ValueSource(strings = { ";", "[", "]", "/", "?", "\"\\r\"", "\"\\n\"", "\"\\f\"", "\"\\b\"", "0XABCD" })
    void test_has_next_valid_tokens(String input) {
        Tokenizer tokenizer = TokenizerABAC.fromString(input);
        assertTrue(tokenizer.hasNext());
    }

    @Test
    void test_has_next_bad_character() {
        Tokenizer tokenizer = TokenizerABAC.fromString("©");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 1] Bad character: ©", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = { "#a\n\n", "#comment\n", "#comment\n\n" })
    void test_has_next_skips_comments(String input) {
        Tokenizer tokenizer = TokenizerABAC.fromString(input);
        assertFalse(tokenizer.hasNext());
    }

    @Test
    void test_has_next_broken_token() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"�");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 3] Broken token: �", exception.getMessage());
    }

    @Test
    void test_has_next_broken_token_new_line() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\n");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertTrue(exception.getMessage().contains("[line: 2, col: 1 ] Broken token (newline): "));
    }

    @Test
    void test_has_next_read_escape_backslash() {
        Tokenizer tokenizer = TokenizerABAC.fromString("\"\\");
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals("[col: 3] Escape sequence not completed", exception.getMessage());
    }

    @Test
    void test_close() {
        Tokenizer tokenizer = TokenizerABAC.fromString("");
        tokenizer.close();
        assertThrows(NullPointerException.class, tokenizer::hasNext);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "\"\"\"|[col: 4] Broken long string",
            "\"\"\"�|[col: 5] Broken long string"
    })
    void test_has_next_broken_long_strings(String input, String expectedMessage) {
        Tokenizer tokenizer = TokenizerABAC.fromString(input);
        Exception exception = assertThrows(AttributeSyntaxError.class, tokenizer::hasNext);
        assertEquals(expectedMessage, exception.getMessage());
    }

    @Test
    void test_line_mode_newline_token() {
        Tokenizer tokenizer = TokenizerABAC.create().fromString("a\n\nb").lineMode(true).build();
        assertEquals(TokenType.WORD, tokenizer.next().getType());
        assertEquals(TokenType.NL, tokenizer.next().getType());
        assertEquals(TokenType.WORD, tokenizer.next().getType());
        assertFalse(tokenizer.hasNext());
    }

    @Test
    void test_line_mode_single_newline_token() {
        Tokenizer tokenizer = TokenizerABAC.create().fromString("a\nb").lineMode(true).build();
        assertEquals(TokenType.WORD, tokenizer.next().getType());
        assertEquals(TokenType.NL, tokenizer.next().getType());
        assertEquals(TokenType.WORD, tokenizer.next().getType());
        assertFalse(tokenizer.hasNext());
    }

}
