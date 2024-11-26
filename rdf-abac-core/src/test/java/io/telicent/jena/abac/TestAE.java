package io.telicent.jena.abac;

import com.google.protobuf.Value;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.jena.abac.attributes.syntax.AE_Allow;
import io.telicent.jena.abac.attributes.syntax.AE_Deny;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TestAE {

    @Test
    public void ae_parse_attribute_value() {
        assertNull(AE.parseAttrValue(null));
    }

    @Test
    public void ae_parse_expression_list() {
        assertEquals(List.of(), AE.parseExprList(null));
    }

    @Test
    public void ae_parse_attribute_value_list() {
        assertEquals(List.of(), AE.parseAttrValueList(null));
    }

    @Test
    public void ae_parse_value_term_list() {
        ABAC.LEGACY = false;
        List<ValueTerm> expected = new ArrayList<>(List.of(ValueTerm.value("test")));
        assertEquals(expected, AE.parseValueTermList("test"));
    }

    @Test
    public void ae_serialize() {
        assertEquals("*, !", AE.serialize(List.of(AE_Allow.value(), AE_Deny.value())));
    }

    @Test
    public void ae_eval_true() {
        ValueTerm expected = ValueTerm.value(true);
        ValueTerm actual = AE.eval("attr=1","attr=1");
        assertEquals(expected, actual);
    }

    @Test
    public void ae_eval_false() {
        ValueTerm expected = ValueTerm.value(false);
        ValueTerm actual = AE.eval("attr=1","attr=2");
        assertEquals(expected, actual);
    }

    @Test
    public void ae_eval_null_hierarchy() {
        ValueTerm expected = ValueTerm.value(true);
        ValueTerm actual = AE.eval("attr=1","attr=1" , null);
        assertEquals(expected, actual);
    }

}
