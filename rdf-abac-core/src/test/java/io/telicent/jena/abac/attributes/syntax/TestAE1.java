package io.telicent.jena.abac.attributes.syntax;

import io.telicent.jena.abac.attributes.AttributeExpr;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.attributes.VisitorAttrExpr;
import io.telicent.jena.abac.core.CxtABAC;
import org.apache.jena.atlas.io.IndentedWriter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class TestAE1 {

    @Test
    public void test_print() {
        TestExpression testExpression = new TestExpression(AE_Allow.value());
        IndentedWriter mockWriter = mock(IndentedWriter.class);
        testExpression.print(mockWriter);
        verify(mockWriter, times(2)).write(anyString());
    }

    @Test
    public void test_to_string() {
        TestExpression testExpression = new TestExpression(AE_Allow.value());
        assertEquals("( *)", testExpression.toString());
    }

    static class TestExpression extends AE1 {
        protected TestExpression(AttributeExpr attrExpr) {
            super(attrExpr);
        }

        @Override
        protected ValueTerm eval(ValueTerm subValue, CxtABAC cxt) {
            return null;
        }

        @Override
        protected String sym() {
            return "";
        }

        @Override
        public void visitor(VisitorAttrExpr visitor) {}

    }
}
