package io.telicent.jena.abac.attributes;

import io.telicent.jena.abac.Hierarchy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({ "java:S125", "java:S1481", "java:S1135" })
class TestAttributeParserEngine {

    @Test
    void testAttributeValue01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> AttributeParser.parseAttrValue(""));
        assertEquals("END", exception.getMessage());
    }

    @Test
    void testHierarchy01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> AttributeParser.parseHierarchy(""));
        assertEquals("END", exception.getMessage());
    }

    @Test
    void testHierarchy02() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class,
                                                      () -> parseHierarchy("status public, confidential, sensitive, private"));
        assertEquals("Expected ':' after attribute name in hierarchy: [WORD:public]", exception.getMessage());
    }

    @Test
    void testReadExprOr01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> parseExpression(""));
        assertEquals("END", exception.getMessage());
    }

    @Test
    void testReadExprAnd01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class,
                                                      () -> parseExpression("(a & b) | (a & b) |"));
        assertEquals("END", exception.getMessage());
    }

    @Test
    void testReadExprUnary01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class,
                                                      () -> parseExpression("(a & b | \"*\""));
        assertEquals("No RPAREN: [LPAREN:(]", exception.getMessage());
    }

    @Test
    void testReadExprUnary02() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> parseExpression("(a }"));
        assertEquals("Expected RPAREN: [RBRACE:}]", exception.getMessage());
    }

    @Test
    void testReadExprUnary03() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> parseExpression("abc & {"));
        assertEquals("No RBRACE: [LBRACE:{]", exception.getMessage());
    }

    @Test
    void testReadExprUnary04() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> parseExpression("a & { }"));
        assertEquals("Expected WORD after: [LBRACE:{]", exception.getMessage());
    }


    @Test
    void testReadExprUnary05() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> parseExpression("{a & b"));
        assertEquals("Expected RBRACE: [AMPERSAND:&]", exception.getMessage());
    }

    @Test
    void testReadExprUnary06() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> parseExpression("{word"));
        assertEquals("No RBRACE: [LBRACE:{]", exception.getMessage());
    }

    @Test
    void testReadExprUnary07_notRecognised() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> parseExpression("?"));
        assertEquals("Not recognized: [QMARK:?]", exception.getMessage());
    }

    @Test
    void testReadExprRel01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> parseExpression("{a} | "));
        assertEquals("END", exception.getMessage());
    }

    //TODO
    // I can't figure out how to trigger the default case - seems to be covered by other exceptions"
//    @Test
//    public void testReadExprRel03() {
//        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
//            AttributeParserEngine aep = new AttributeParserEngine("a ^ b == c");
//            AttributeExpr ae1 = aep.attributeExpression();
//        });
//        assertEquals("Not a relationship operator: '", exception.getMessage());
//    }

    @Test
    void testReadAttributeValue01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> parseExpression("a & b = {"));
        assertEquals("Expected an attribute value: Not recognized: [LBRACE:{]", exception.getMessage());
    }

    @Test
    void testReadNumericalAttributeNames() {
        testAttributeValue("123");
        testAttributeValue("0");
        testAttributeValue("999");
        testAttributeValue("1.0");
        testAttributeValue("3.14");
        testAttributeValuePair("123=value");
        testAttributeValuePair("1.0=test");
        //testAttributeValue("-1"); // We drop the leading sign on values - something to investigate later.
        //testAttributeValuePair("-11.0=test");
    }

    private void testAttributeValue(String input) {
        AttributeParserEngine aep = new AttributeParserEngine(input);
        AttributeValue av = aep.attributeValue();
        assertEquals(input, av.attribute().name(), "Was supposed to be " + input + " was " + av.attribute().name());
    }

    private void testAttributeValuePair(String input) {
        AttributeParserEngine aep = new AttributeParserEngine(input);
        AttributeValue av = aep.attributeValue();
        assertNotNull(av);

        String[] parts = input.split("=", 2);
        String expectedAttribute = parts[0];
        String expectedValue = parts[1];

        assertEquals(expectedAttribute, av.attribute().name(), "Was supposed to be " + expectedAttribute + " was " + av.attribute().name());

        ValueTerm value = av.value();
        assertNotNull(value);
        assertEquals(expectedValue, value.toString(), "Was supposed to be " + expectedValue + " was " + value);
    }

    private static AttributeExpr parseExpression(String input) {
        return new AttributeParserEngine(input).attributeExpression();
    }

    private static Hierarchy parseHierarchy(String input) {
        return new AttributeParserEngine(input).hierarchy();
    }

}
