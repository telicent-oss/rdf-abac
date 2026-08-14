package io.telicent.jena.abac.attributes.syntax;

import io.telicent.jena.abac.attributes.VisitorAttrExpr;
import io.telicent.jena.abac.core.CxtABAC;
import org.apache.jena.atlas.lib.NotImplemented;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("java:S6213")
public class TestAE_Var {

    private final CxtABAC mockContext = mock(CxtABAC.class);

    @Test
    void test_eval_exception() {
        AE_Var expr = new AE_Var("test");
        assertThrows(NotImplemented.class, () -> expr.eval(mockContext));
    }

    @Test
    void test_visitor() {
        AE_Var expr = new AE_Var("test");
        VisitorAttrExpr mockVistorAttrExpr = mock(VisitorAttrExpr.class);
        expr.visitor(mockVistorAttrExpr);
        verify(mockVistorAttrExpr).visit(any(AE_Var.class));
    }

    @Test
    void test_equals_same() {
        AE_Var expr = new AE_Var("test");
        assertEquals(expr, expr); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_identical() {
        AE_Var var1 = new AE_Var("test");
        AE_Var var2 = new AE_Var("test");
        assertEquals(var1, var2); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_null() {
        AE_Var expr = new AE_Var("test");
        assertNotEquals(expr, null); // we are specifically testing the equals method here
    }

    @Test
    void test_equals_different_class() {
        AE_Var expr = new AE_Var("test");
        assertNotEquals(expr, "test"); // we are specifically testing the equals method here
    }

}
