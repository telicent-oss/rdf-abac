package io.telicent.jena.abac.core;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.apache.jena.atlas.web.HttpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class TestAttributeStoreRemote {

    private HttpClient mockHttpClient;
    private HttpResponse mockHttpResponse;

    @BeforeEach
    public void setUp() {
        mockHttpClient = Mockito.mock(HttpClient.class);
        mockHttpResponse = Mockito.mock(HttpResponse.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_attributes_missing_user_param() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(404);
        String initialString = "text";
        InputStream testStream = new ByteArrayInputStream(initialString.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        Exception exception = assertThrows(AuthzException.class, () -> {
            asr.attributes("user1");
        });
        assertTrue(exception.getMessage().contains("Parameter {user} not found"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_attributes_404_response() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(404);
        String initialString = "text";
        InputStream testStream = new ByteArrayInputStream(initialString.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        AttributeValueSet avs = asr.attributes("user1");
        assertNull(avs);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_attributes_not_json_response() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "not json";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        AttributeValueSet avs = asr.attributes("user1");
        assertNull(avs);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_attributes_not_correct_json() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{ \"k\": \"v\" }";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        AttributeValueSet avs = asr.attributes("user1");
        assertNull(avs);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_attributes_not_json_array() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{ \"attributes\": \"v\" }";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        AttributeValueSet avs = asr.attributes("user1");
        assertNull(avs);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_attributes_not_json_string_array() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{ \"attributes\": [ 0 ] }";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        AttributeValueSet actual = asr.attributes("user1");
        AttributeValueSet expected = AttributeValueSet.of(List.of());
        assertEquals(expected, actual);
    }


    @Test
    @SuppressWarnings("unchecked")
    public void test_attributes_json_array_ok() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{ \"attributes\": [\"v\"] }";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        AttributeValueSet actual = asr.attributes("user1");
        AttributeValueSet expected = AttributeValueSet.of(List.of(AttributeValue.of("v", ValueTerm.TRUE)));
        assertEquals(expected, actual);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_attributes_json_array_exception() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{ \"attributes\": [\"v>1\"] }";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        Exception exception = assertThrows(AttributeSyntaxError.class, () -> {
            asr.attributes("user1");
        });
        assertTrue(exception.getMessage().contains("More tokens: [GT:>]"));
    }

    @Test
    public void test_attributes_http_exception() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenThrow(new HttpException("Error"));
        assertNull(asr.attributes("user1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_has_hierarchy_true() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{ \"tiers\": [\"v\"] }";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        assertTrue(asr.hasHierarchy(new Attribute("a")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_has_hierarchy_empty() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{ \"tiers\": [] }";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        assertFalse(asr.hasHierarchy(new Attribute("a")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_has_hierarchy_404() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(404);
        String responseBody = "text";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        assertFalse(asr.hasHierarchy(new Attribute("a")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_has_hierarchy_not_json() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "text";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        assertFalse(asr.hasHierarchy(new Attribute("a")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_has_hierarchy_not_correct_json() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{\"k\":\"v\"}}";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        assertFalse(asr.hasHierarchy(new Attribute("a")));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void test_has_hierarchy_not_json_array() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{\"tiers\":\"v\"}}";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        assertFalse(asr.hasHierarchy(new Attribute("a")));
    }

    @Test
    public void test_has_hierarchy_http_exception() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenThrow(new HttpException("Error"));
        assertFalse(asr.hasHierarchy(new Attribute("a")));
    }

    @Test
    public void test_users() {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "", mockHttpClient);
        assertEquals(Set.of(), asr.users());
    }

}
