package io.telicent.jena.abac.attributes.syntax;

import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.Operator;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.core.CxtABAC;
import org.apache.jena.atlas.lib.NotImplemented;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAttrExprEvaluator {

    private final CxtABAC mockContext = mock(CxtABAC.class);
    private final Attribute testAttribute = Attribute.create("test");

    @Test
    public void test_eval_null() {
        when(mockContext.getValue(testAttribute)).thenReturn(null);
        AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        assertEquals(ValueTerm.FALSE, AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext));
    }

    @Test
    public void test_eval_empty() {
        when(mockContext.getValue(testAttribute)).thenReturn(new ArrayList<>());
        AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        assertEquals(ValueTerm.FALSE, AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext));
    }

    @Test
    public void test_eval_ne_true() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertEquals(ValueTerm.TRUE, AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.FALSE, mockContext));
    }

    @Test
    public void test_eval_ne_false() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertEquals(ValueTerm.TRUE, AttrExprEvaluator.eval(Operator.NE, Attribute.create("test"), ValueTerm.TRUE, mockContext));
    }

    @Test
    public void test_eval_exception_ge() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertThrows(NotImplemented.class, () -> {
            AttrExprEvaluator.eval(Operator.GE, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        });
    }

    @Test
    public void test_eval_exception_gt() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertThrows(NotImplemented.class, () -> {
            AttrExprEvaluator.eval(Operator.GT, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        });
    }

    @Test
    public void test_eval_exception_le() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertThrows(NotImplemented.class, () -> {
            AttrExprEvaluator.eval(Operator.LE, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        });
    }

    @Test
    public void test_eval_exception_lt() {
        ValueTerm[] array = {ValueTerm.TRUE, ValueTerm.FALSE};
        Collection<ValueTerm> valueTerms = Arrays.asList(array);
        when(mockContext.getValue(testAttribute)).thenReturn(valueTerms);
        assertThrows(NotImplemented.class, () -> {
            AttrExprEvaluator.eval(Operator.LT, Attribute.create("test"), ValueTerm.TRUE, mockContext);
        });
    }

    @Test
    public void test_eval_and_true() {
        assertEquals(ValueTerm.TRUE, AttrExprEvaluator.evalAnd(AE_Allow.value(),AE_Allow.value(),mockContext));
    }

    @Test
    public void test_eval_and_false() {
        assertEquals(ValueTerm.FALSE, AttrExprEvaluator.evalAnd(AE_Deny.value(),AE_Deny.value(),mockContext));
    }

}
