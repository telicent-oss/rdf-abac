package io.telicent.jena.abac.core;

import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.AttributeSyntaxError;
import io.telicent.jena.abac.attributes.AttributeValue;
import io.telicent.jena.abac.attributes.ValueTerm;
import org.apache.jena.atlas.web.HttpException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestAttributeStoreRemote {

    private HttpClient mockHttpClient;
    private HttpResponse mockHttpResponse;

    @BeforeEach
    void setUp() {
        mockHttpClient = mock(HttpClient.class);
        mockHttpResponse = mock(HttpResponse.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_attributes_missing_user_param() throws Exception {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/", "", mockHttpClient);
        when(mockHttpClient.send(any(), any())).thenReturn(mockHttpResponse);
        when(mockHttpResponse.statusCode()).thenReturn(404);
        String initialString = "text";
        InputStream testStream = new ByteArrayInputStream(initialString.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        Exception exception = assertThrows(AuthzException.class, () -> asr.attributes("user1"));
        assertTrue(exception.getMessage().contains("Parameter {user} not found"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_attributes_404_response() {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        when(mockHttpResponse.statusCode()).thenReturn(404);
        String initialString = "text";
        InputStream testStream = new ByteArrayInputStream(initialString.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        AttributeValueSet avs = asr.attributes("user1");
        assertNull(avs);
    }

    @ParameterizedTest
    @ValueSource(strings = { "not json", "{ \"k\": \"v\" }", "{ \"attributes\": \"v\" }" })
    @SuppressWarnings("unchecked")
    void test_attributes_invalid_responses_return_null(String responseBody) {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        when(mockHttpResponse.statusCode()).thenReturn(200);
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        AttributeValueSet avs = asr.attributes("user1");
        assertNull(avs);
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_attributes_not_json_string_array() {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
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
    void test_attributes_json_array_ok() {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
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
    void test_attributes_json_array_exception() {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{ \"attributes\": [\"v>1\"] }";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        Exception exception = assertThrows(AttributeSyntaxError.class, () -> asr.attributes("user1"));
        assertTrue(exception.getMessage().contains("More tokens: [GT:>]"));
    }

    @Test
    void test_attributes_http_exception() {
        AttributesStoreRemote asr = new AttributesStoreRemote("http://localhost:8080/user/{user}", "", mockHttpClient);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.failedFuture(new HttpException("Error")));
        assertNull(asr.attributes("user1"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void test_has_hierarchy_true() {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        when(mockHttpResponse.statusCode()).thenReturn(200);
        String responseBody = "{ \"tiers\": [\"v\"] }";
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        assertTrue(asr.hasHierarchy(new Attribute("a")));
    }

    @ParameterizedTest
    @MethodSource("falseHierarchyResponses")
    @SuppressWarnings("unchecked")
    void test_has_hierarchy_false_cases(int statusCode, String responseBody) {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.completedFuture(mockHttpResponse));
        when(mockHttpResponse.statusCode()).thenReturn(statusCode);
        InputStream testStream = new ByteArrayInputStream(responseBody.getBytes());
        when(mockHttpResponse.body()).thenReturn(testStream);
        assertFalse(asr.hasHierarchy(new Attribute("a")));
    }

    @Test
    void test_has_hierarchy_http_exception() {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "http://localhost:8080/hierarchy/{name}", mockHttpClient);
        when(mockHttpClient.sendAsync(any(), any())).thenReturn(CompletableFuture.failedFuture(new HttpException("Error")));
        assertFalse(asr.hasHierarchy(new Attribute("a")));
    }

    @Test
    void test_users() {
        AttributesStoreRemote asr = new AttributesStoreRemote("", "", mockHttpClient);
        assertEquals(Set.of(), asr.users());
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> falseHierarchyResponses() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of(200, "{ \"tiers\": [] }"),
                org.junit.jupiter.params.provider.Arguments.of(404, "text"),
                org.junit.jupiter.params.provider.Arguments.of(200, "text"),
                org.junit.jupiter.params.provider.Arguments.of(200, "{\"k\":\"v\"}}"),
                org.junit.jupiter.params.provider.Arguments.of(200, "{\"tiers\":\"v\"}}"));
    }

}
