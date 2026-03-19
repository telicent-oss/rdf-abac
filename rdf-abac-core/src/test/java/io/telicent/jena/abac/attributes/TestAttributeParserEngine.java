package io.telicent.jena.abac.attributes;

import io.telicent.jena.abac.Hierarchy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestAttributeParserEngine {

    @Test
    public void testAttributeValue01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeValue av1 = AttributeParser.parseAttrValue("");
        });
        assertEquals("END", exception.getMessage());
    }

    @Test
    public void testHierarchy01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            Hierarchy h1 = AttributeParser.parseHierarchy("");
        });
        assertEquals("END", exception.getMessage());
    }

    @Test
    public void testHierarchy02() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("status public, confidential, sensitive, private");
            Hierarchy h1 = aep.hierarchy();
        });
        assertEquals("Expected ':' after attribute name in hierarchy: [WORD:public]", exception.getMessage());
    }

    @Test
    public void testReadExprOr01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("END", exception.getMessage());
    }

    @Test
    public void testReadExprAnd01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("(a & b) | (a & b) |");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("END", exception.getMessage());
    }

    @Test
    public void testReadExprUnary01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("(a & b | \"*\"");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("No RPAREN: [LPAREN:(]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary02() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("(a }");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Expected RPAREN: [RBRACE:}]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary03() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("abc & {");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("No RBRACE: [LBRACE:{]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary04() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("a & { }");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Expected WORD after: [LBRACE:{]", exception.getMessage());
    }


    @Test
    public void testReadExprUnary05() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("{a & b");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Expected RBRACE: [AMPERSAND:&]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary06() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("{word");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("No RBRACE: [LBRACE:{]", exception.getMessage());
    }

    @Test
    public void testReadExprUnary07_notRecognised() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("?");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Not recognized: [QMARK:?]", exception.getMessage());
    }

    @Test
    public void testReadExprRel01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("{a} | ");
            AttributeExpr ae1 = aep.attributeExpression();
        });
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


    // numeric attribute names are temporarily allowed

//    @Test
//    public void testReadAttribute01() {
//        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
//            AttributeParserEngine aep = new AttributeParserEngine("1.0:");
//            AttributeValue av1 = aep.attributeValue();
//        });
//        assertEquals("Expected an attribute: Got a number: [DECIMAL:1.0]", exception.getMessage());
//    }

    @Test
    public void testReadAttributeValue01() {
        AttributeSyntaxError exception = assertThrows(AttributeSyntaxError.class, () -> {
            AttributeParserEngine aep = new AttributeParserEngine("a & b = {");
            AttributeExpr ae1 = aep.attributeExpression();
        });
        assertEquals("Expected an attribute value: Not recognized: [LBRACE:{]", exception.getMessage());
    }

    @Test
    public void testReadNumericalAttributeNames() {
        testAttributeValue("123");
        testAttributeValue("0");
        testAttributeValue("999");
        testAttributeValue("1.0");
        testAttributeValue("3.14");
        testAttributeValuePair("123=value");
        testAttributeValuePair("1.0=test");
        testAttributeValue("-1");
        testAttributeValuePair("-11.0=test");        
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

}
