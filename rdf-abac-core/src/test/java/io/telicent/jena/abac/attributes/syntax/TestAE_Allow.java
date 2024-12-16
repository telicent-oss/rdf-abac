package io.telicent.jena.abac.attributes.syntax;

import io.telicent.jena.abac.attributes.VisitorAttrExpr;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class TestAE_Allow {

    @Test
    public void test_visitor() {
        AE_Allow allow = (AE_Allow) AE_Allow.value();
        VisitorAttrExpr mockVistorAttrExpr = mock(VisitorAttrExpr.class);
        allow.visitor(mockVistorAttrExpr);
        verify(mockVistorAttrExpr).visit(any(AE_Allow.class));
    }
}
