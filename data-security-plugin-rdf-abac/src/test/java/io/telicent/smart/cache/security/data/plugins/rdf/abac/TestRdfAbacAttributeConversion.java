/**
 * Copyright (C) Telicent Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.telicent.smart.cache.security.data.plugins.rdf.abac;

import io.jsonwebtoken.Jws;
import io.telicent.jena.abac.AttributeValueSet;
import io.telicent.jena.abac.attributes.Attribute;
import io.telicent.jena.abac.attributes.ValueTerm;
import io.telicent.smart.cache.security.data.DataAccessAuthorizer;
import io.telicent.smart.cache.security.data.plugins.failsafe.FailSafeAuthorizer;
import io.telicent.smart.cache.security.data.requests.MinimalRequestContext;
import io.telicent.smart.cache.security.data.requests.RequestContext;
import io.telicent.smart.caches.configuration.auth.UserInfo;
import io.telicent.smart.caches.configuration.auth.UserInfoLookupException;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;

@SuppressWarnings("unchecked")
public class TestRdfAbacAttributeConversion {

    private final RdfAbacPlugin plugin = new RdfAbacPlugin();

    @Test
    public void givenMinimalUserInfo_whenPreparingAuthorizerToUserAttributes_thenNoAttributes() throws UserInfoLookupException,
            IOException {
        // Given
        UserInfo info = UserInfo.builder().sub("test").preferredName("Mr T. Test").build();
        RequestContext context =
                MinimalRequestContext.builder().username("Mr T. Test").userInfo(info).jwt(mock(Jws.class)).build();

        // When
        try (RdfAbacAuthorizer authorizer = (RdfAbacAuthorizer) this.plugin.prepareAuthorizer(context)) {
            // Then
            AttributeValueSet attrs = authorizer.userAttributes();
            Assert.assertNotNull(attrs);
            Assert.assertTrue(attrs.isEmpty());
        }
    }

    private void verifyMissingAttributes(AttributeValueSet attrs, String... missingAttributes) {
        for (String missing : missingAttributes) {
            Attribute a = Attribute.create(missing);
            Assert.assertFalse(attrs.hasAttribute(a));
        }
    }

    private void verifyAttributes(AttributeValueSet attrs, Map<String, Object> attributes) {
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            Attribute a = Attribute.create(entry.getKey());
            Assert.assertTrue(attrs.hasAttribute(a));
            Collection<ValueTerm> values = attrs.get(a);

            if (entry.getValue() instanceof Collection<?> expected) {
                Assert.assertEquals(expected.size(), values.size());
                List<String> expectedValues = expected.stream().map(Object::toString).sorted().toList();
                List<String> actualValues = values.stream().map(ValueTerm::getString).sorted().toList();
                Assert.assertEquals(expectedValues, actualValues);
            } else {
                Assert.assertNotEquals(values.size(), 0);
                verifyValue(entry.getValue(), values.iterator().next());
            }
        }
    }

    private static void verifyValue(Object expected, ValueTerm actual) {
        if (expected instanceof Boolean b) {
            Assert.assertEquals(b, actual.getBoolean());
        } else {
            Assert.assertEquals(expected.toString(), actual.getString());
        }
    }

    @Test
    public void givenBasicUserInfo_whenPreparingAuthorizerToUserAttributes_thenAttributes() {
        // Given
        Map<String, Object> attributes =
                Map.of("email", "test@example.org", "age", 42, "nationality", "GBR", "active", true);
        UserInfo info = UserInfo.builder().preferredName("Mr T. Test").attributes(attributes).build();
        RequestContext context =
                MinimalRequestContext.builder().username("Mr T. Test").userInfo(info).jwt(mock(Jws.class)).build();

        // When
        try (RdfAbacAuthorizer authorizer = (RdfAbacAuthorizer) this.plugin.prepareAuthorizer(context)) {
            // Then
            AttributeValueSet attrs = authorizer.userAttributes();
            Assert.assertNotNull(attrs);
            Assert.assertFalse(attrs.isEmpty());
            verifyAttributes(attrs, attributes);
        }
    }

    @Test
    public void givenUserInfoWithListAttribute_whenPreparingAuthorizerToUserAttributes_thenAllAttributesMaterialised() {
        // Given
        Map<String, Object> attributes =
                Map.of("email", List.of("test@example.org", "test@personal.org"), "age", 42, "nationality",
                       List.of("GBR", "US"), "active", true);
        UserInfo info = UserInfo.builder().preferredName("Mr T. Test").attributes(attributes).build();
        RequestContext context =
                MinimalRequestContext.builder().username("Mr T. Test").userInfo(info).jwt(mock(Jws.class)).build();

        // When
        try (RdfAbacAuthorizer authorizer = (RdfAbacAuthorizer) this.plugin.prepareAuthorizer(context)) {
            // Then
            AttributeValueSet attrs = authorizer.userAttributes();
            Assert.assertNotNull(attrs);
            Assert.assertFalse(attrs.isEmpty());
            verifyAttributes(attrs, attributes);
        }
    }

    @Test
    public void givenUserInfoWithNonConvertibleAttributes_whenPreparingAuthorizerToUserAttributes_thenAttributesIgnored() {
        // Given
        Map<String, Object> attributes = Map.of("ignored", new Object());
        UserInfo info = UserInfo.builder().preferredName("Mr T. Test").attributes(attributes).build();
        RequestContext context =
                MinimalRequestContext.builder().username("Mr T. Test").userInfo(info).jwt(mock(Jws.class)).build();

        // When
        try (RdfAbacAuthorizer authorizer = (RdfAbacAuthorizer) this.plugin.prepareAuthorizer(context)) {
            // Then
            AttributeValueSet attrs = authorizer.userAttributes();
            Assert.assertNotNull(attrs);
            Assert.assertTrue(attrs.isEmpty());
            verifyMissingAttributes(attrs, "ignored");
        }
    }

    @Test
    public void givenUserInfoWithNullAttributeValue_whenPreparingAuthorizerToUserAttributes_thenAttributeIgnored() {
        // Given
        // NB - HashMap rather than Map.of because Map.of rejects null values, and a null is exactly what is under
        //      test here: an identity provider that returns a claim with no value
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("ignored", null);
        UserInfo info = UserInfo.builder().preferredName("Mr T. Test").attributes(attributes).build();
        RequestContext context =
                MinimalRequestContext.builder().username("Mr T. Test").userInfo(info).jwt(mock(Jws.class)).build();

        // When
        try (RdfAbacAuthorizer authorizer = (RdfAbacAuthorizer) this.plugin.prepareAuthorizer(context)) {
            // Then
            AttributeValueSet attrs = authorizer.userAttributes();
            Assert.assertNotNull(attrs);
            Assert.assertTrue(attrs.isEmpty());
            verifyMissingAttributes(attrs, "ignored");
        }
    }

    @Test
    public void givenUserInfoWithMapAttribute_whenPreparingAuthorizerToUserAttributes_thenAttributesFlattened() {
        // Given
        Map<String, Object> attributes = Map.of("email", "test@example.org", "employment",
                                                Map.of("company", "Telicent Ltd", "title", "QA Engineer", "department",
                                                       "R&D", "manages", List.of("Adam", "Bob", "Eve")));
        UserInfo info = UserInfo.builder().preferredName("Mr T. Test").attributes(attributes).build();
        RequestContext context =
                MinimalRequestContext.builder().username("Mr T. Test").userInfo(info).jwt(mock(Jws.class)).build();

        // When
        try (RdfAbacAuthorizer authorizer = (RdfAbacAuthorizer) this.plugin.prepareAuthorizer(context)) {
            // Then
            AttributeValueSet attrs = authorizer.userAttributes();
            Assert.assertNotNull(attrs);
            Assert.assertFalse(attrs.isEmpty());
            Map<String, Object> expected =
                    Map.of("email", "test@example.org", "employment.company", "Telicent Ltd", "employment.title",
                           "QA Engineer", "employment.department", "R&D", "employment.manages",
                           List.of("Adam", "Bob", "Eve"));
            verifyAttributes(attrs, expected);
        }
    }

    @Test
    public void givenNoUserInfo_whenPreparingAuthorizer_thenFailSafeAuthorizerReturned() {
        // Given
        RequestContext context = MinimalRequestContext.builder().username("test").jwt(mock(Jws.class)).build();

        // When
        try (DataAccessAuthorizer authorizer = this.plugin.prepareAuthorizer(context)) {
            // Then
            Assert.assertTrue(authorizer instanceof FailSafeAuthorizer);
        }
    }
}
