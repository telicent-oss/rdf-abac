/*
 *  Copyright (c) Telicent Ltd.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.telicent.jena.abac.attributes.syntax.tokens;

import org.apache.jena.riot.tokens.Token;
import org.apache.jena.riot.tokens.Tokenizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class TestTokenizerWrapper {

    @Test
    void testDelegatesAllTokenizerOperations() {
        Tokenizer delegate = mock(Tokenizer.class);
        Token token = mock(Token.class);

        when(delegate.getColumn()).thenReturn(7L);
        when(delegate.getLine()).thenReturn(9L);
        when(delegate.hasNext()).thenReturn(true);
        when(delegate.eof()).thenReturn(false);
        when(delegate.next()).thenReturn(token);
        when(delegate.peek()).thenReturn(token);

        TokenizerWrapper wrapper = new TokenizerWrapper(123, delegate);

        assertEquals(7L, wrapper.getColumn());
        assertEquals(9L, wrapper.getLine());
        assertTrue(wrapper.hasNext());
        assertFalse(wrapper.eof());
        assertSame(token, wrapper.next());
        assertSame(token, wrapper.peek());

        wrapper.close();
        verify(delegate).close();
    }
}
