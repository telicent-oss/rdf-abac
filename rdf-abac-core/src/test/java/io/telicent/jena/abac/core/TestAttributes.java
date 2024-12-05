package io.telicent.jena.abac.core;

import io.telicent.jena.abac.attributes.Attribute;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.*;

public class TestAttributes {

    @Test
    public void test_attribute() {
        Attribute expected = new Attribute("test");
        Attribute actual = Attributes.attribute("test");
        assertEquals(expected, actual);
    }

    @Test
    public void test_read_attribute_store() {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store.ttl");
        AttributesStore attributesStore = Attributes.readAttributesStore(attributeStoreUrl.getPath(), null);
        assertNotNull(attributesStore);
    }

    @Test
    public void test_read_attribute_store_with_validation() throws Exception {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store.ttl");
        URL attributeShapesUrl = getClass().getClassLoader().getResource("AttributesShape.ttl");
        AttributesStore attributesStore = Attributes.readAttributesStore(attributeStoreUrl.getPath(), attributeShapesUrl.getPath());
        assertNotNull(attributesStore);
    }

    @Test
    public void test_read_attribute_store_with_validation_invalid() throws Exception {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store-invalid.ttl");
        URL attributeShapesUrl = getClass().getClassLoader().getResource("AttributesShape.ttl");
        Exception exception = assertThrows(AuthzException.class, () -> {
            Attributes.readAttributesStore(attributeStoreUrl.getPath(), attributeShapesUrl.getPath());
        });
        assertEquals("Bad attributes store file", exception.getMessage());
    }

    @Test
    public void test_populate_store_bad_attribute() {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store-bad-attribute.ttl");
        Graph graph = RDFDataMgr.loadGraph(attributeStoreUrl.getPath());
        AttributesStoreModifiable store = new AttributesStoreLocal();
        Exception exception = assertThrows(AuthzException.class, () -> {
            Attributes.populateStore(graph,store);
        });
        assertTrue(exception.getMessage().contains("Bad value for ?attribute"));
    }

    @Test
    public void test_populate_store_bad_user() {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store-bad-user.ttl");
        Graph graph = RDFDataMgr.loadGraph(attributeStoreUrl.getPath());
        AttributesStoreModifiable store = new AttributesStoreLocal();
        Exception exception = assertThrows(AuthzException.class, () -> {
            Attributes.populateStore(graph,store);
        });
        assertTrue(exception.getMessage().contains("Bad value for ?user"));
    }

    @Test
    public void test_string_null() {
        Exception exception = assertThrows(NullPointerException.class, () -> {
            Attributes.string(null);
        });
        assertEquals("Missing string for node", exception.getMessage());
    }

    @Test
    public void test_string_not_a_literal() {
        Exception exception = assertThrows(AuthzException.class, () -> {
            Node uriNode = NodeFactory.createURI("http://example.org/a");
            Attributes.string(uriNode);
        });
        assertEquals("Not a literal string", exception.getMessage());
    }

    @Test
    public void test_string_literal_not_string() {
        Exception exception = assertThrows(AuthzException.class, () -> {
            Node uriNode = NodeFactory.createLiteralByValue(123);
            Attributes.string(uriNode);
        });
        assertEquals("Literal but not a plain string", exception.getMessage());
    }

}
