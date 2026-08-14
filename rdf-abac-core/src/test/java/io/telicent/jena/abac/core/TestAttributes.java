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
    void test_attribute() {
        Attribute expected = new Attribute("test");
        Attribute actual = Attributes.attribute("test");
        assertEquals(expected, actual);
    }

    @Test
    void test_read_attribute_store() {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store.ttl");
        AttributesStore attributesStore = Attributes.readAttributesStore(attributeStoreUrl.getPath(), null);
        assertNotNull(attributesStore);
    }

    @Test
    void test_read_attribute_store_with_validation() {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store.ttl");
        URL attributeShapesUrl = getClass().getClassLoader().getResource("AttributesShape.ttl");
        AttributesStore attributesStore = Attributes.readAttributesStore(attributeStoreUrl.getPath(), attributeShapesUrl.getPath());
        assertNotNull(attributesStore);
    }

    @Test
    void test_read_attribute_store_with_validation_invalid() {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store-invalid.ttl");
        URL attributeShapesUrl = getClass().getClassLoader().getResource("AttributesShape.ttl");
        String attributeStorePath = attributeStoreUrl.getPath();
        String attributeShapesPath = attributeShapesUrl.getPath();
        Exception exception = assertThrows(AuthzException.class,
                                           () -> Attributes.readAttributesStore(attributeStorePath, attributeShapesPath));
        assertEquals("Bad attributes store file", exception.getMessage());
    }

    @Test
    void test_populate_store_bad_attribute() {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store-bad-attribute.ttl");
        Graph graph = RDFDataMgr.loadGraph(attributeStoreUrl.getPath());
        AttributesStoreModifiable store = new AttributesStoreLocal();
        Exception exception = assertThrows(AuthzException.class, () -> Attributes.populateStore(graph, store));
        assertTrue(exception.getMessage().contains("Bad value for ?attribute"));
    }

    @Test
    void test_populate_store_bad_user() {
        URL attributeStoreUrl = getClass().getClassLoader().getResource("test-attribute-store-bad-user.ttl");
        Graph graph = RDFDataMgr.loadGraph(attributeStoreUrl.getPath());
        AttributesStoreModifiable store = new AttributesStoreLocal();
        Exception exception = assertThrows(AuthzException.class, () -> Attributes.populateStore(graph, store));
        assertTrue(exception.getMessage().contains("Bad value for ?user"));
    }

    @Test
    void test_string_null() {
        Exception exception = assertThrows(NullPointerException.class, () -> Attributes.string(null));
        assertEquals("Missing string for node", exception.getMessage());
    }

    @Test
    void test_string_not_a_literal() {
        Node uriNode = NodeFactory.createURI("http://example.org/a");
        Exception exception = assertThrows(AuthzException.class, () -> Attributes.string(uriNode));
        assertEquals("Not a literal string", exception.getMessage());
    }

    @Test
    void test_string_literal_not_string() {
        Node uriNode = NodeFactory.createLiteralByValue(123);
        Exception exception = assertThrows(AuthzException.class, () -> Attributes.string(uriNode));
        assertEquals("Literal but not a plain string", exception.getMessage());
    }

}
