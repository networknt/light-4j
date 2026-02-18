package com.networknt.token.exchange;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests for the VariableResolver utility class.
 */
public class VariableResolverTest {

    @Test
    public void testResolveSimpleVariable() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("accessToken", "my-token-123");

        // Test with sharedVariables. prefix (backward compatible)
        String result = VariableResolver.resolve("Bearer !ref(sharedVariables.accessToken)", variables);
        Assert.assertEquals("Bearer my-token-123", result);
    }

    @Test
    public void testResolveSimpleVariableShorthand() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("accessToken", "my-token-123");

        // Test simplified syntax without prefix
        String result = VariableResolver.resolve("Bearer !ref(accessToken)", variables);
        Assert.assertEquals("Bearer my-token-123", result);
    }

    @Test
    public void testResolveMultipleVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("clientId", "client-abc");
        variables.put("scope", "read write");

        // Test with simplified syntax
        String result = VariableResolver.resolve(
                "client=!ref(clientId)&scope=!ref(scope)",
                variables
        );
        Assert.assertEquals("client=client-abc&scope=read write", result);
    }

    @Test
    public void testResolveWithNoVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("token", "abc");

        String result = VariableResolver.resolve("static-value", variables);
        Assert.assertEquals("static-value", result);
    }

    @Test
    public void testResolveWithNullValue() {
        Map<String, Object> variables = new HashMap<>();
        // Variable not set

        String result = VariableResolver.resolve("Bearer !ref(sharedVariables.missingToken)", variables);
        Assert.assertEquals("Bearer ", result);
    }

    @Test
    public void testResolveWithLongValue() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("expiration", 1234567890L);

        String result = VariableResolver.resolve("exp=!ref(sharedVariables.expiration)", variables);
        Assert.assertEquals("exp=1234567890", result);
    }

    @Test
    public void testResolveWithCharArrayValue() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("password", "secret123".toCharArray());

        String result = VariableResolver.resolve("pass=!ref(sharedVariables.password)", variables);
        Assert.assertEquals("pass=secret123", result);
    }

    @Test
    public void testResolveMapWithVariables() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("clientId", "my-client");
        variables.put("clientSecret", "my-secret");

        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("client_id", "!ref(sharedVariables.clientId)");
        templateMap.put("client_secret", "!ref(sharedVariables.clientSecret)");
        templateMap.put("grant_type", "client_credentials");

        Map<String, String> resolved = VariableResolver.resolveMap(templateMap, variables);

        Assert.assertEquals("my-client", resolved.get("client_id"));
        Assert.assertEquals("my-secret", resolved.get("client_secret"));
        Assert.assertEquals("client_credentials", resolved.get("grant_type"));
    }

    @Test
    public void testResolveMapWithNullInput() {
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> resolved = VariableResolver.resolveMap(null, variables);
        Assert.assertNotNull(resolved);
        Assert.assertTrue(resolved.isEmpty());
    }

    @Test
    public void testExtractDestinationVariable() {
        String destination = "!ref(sharedVariables.accessToken)";
        String variableName = VariableResolver.extractDestinationVariable(destination);
        Assert.assertEquals("accessToken", variableName);
    }

    @Test
    public void testExtractDestinationVariableNested() {
        String destination = "!ref(sharedVariables.myCustomVar)";
        String variableName = VariableResolver.extractDestinationVariable(destination);
        Assert.assertEquals("myCustomVar", variableName);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testExtractDestinationVariableInvalid() {
        VariableResolver.extractDestinationVariable("not-a-ref-pattern");
    }

    // Tests for new request context reference feature

    @Test
    public void testResolveRequestHeaderReference() {
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer original-token");
        headers.put("x-correlation-id", "corr-12345");
        RequestContext requestContext = new RequestContext("test-schema", headers, new HashMap<>(), "/api/v1/test");

        String result = VariableResolver.resolve(
                "Original auth: !ref(request.header.authorization)",
                variables,
                requestContext
        );
        Assert.assertEquals("Original auth: Bearer original-token", result);
    }

    @Test
    public void testResolveRequestHeaderReferenceCaseInsensitive() {
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("x-custom-header", "custom-value");
        RequestContext requestContext = new RequestContext("test-schema", headers, new HashMap<>(), "/api/test");

        // Reference using different case
        String result = VariableResolver.resolve(
                "Header: !ref(request.header.x-custom-header)",
                variables,
                requestContext
        );
        Assert.assertEquals("Header: custom-value", result);
    }

    @Test
    public void testResolveRequestQueryReference() {
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("clientid", "query-client-123");
        queryParams.put("scope", "read");
        RequestContext requestContext = new RequestContext("test-schema", new HashMap<>(), queryParams, "/api/test");

        String result = VariableResolver.resolve(
                "Client from query: !ref(request.query.clientId)",
                variables,
                requestContext
        );
        Assert.assertEquals("Client from query: query-client-123", result);
    }

    @Test
    public void testResolveRequestQueryReferenceCaseInsensitive() {
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("clientid", "query-client-456");
        RequestContext requestContext = new RequestContext("test-schema", new HashMap<>(), queryParams, "/api/test");

        // Reference using different case (lowercase)
        String result = VariableResolver.resolve(
                "Client: !ref(request.query.clientid)",
                variables,
                requestContext
        );
        Assert.assertEquals("Client: query-client-456", result);
    }

    @Test
    public void testResolveRequestPathReference() {
        Map<String, Object> variables = new HashMap<>();
        RequestContext requestContext = new RequestContext("test-schema", new HashMap<>(), new HashMap<>(), "/api/v1/users/123");

        String result = VariableResolver.resolve(
                "Path: !ref(request.path)",
                variables,
                requestContext
        );
        Assert.assertEquals("Path: /api/v1/users/123", result);
    }

    @Test
    public void testResolveMixedReferences() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("accessToken", "new-token");

        Map<String, String> headers = new HashMap<>();
        headers.put("x-request-id", "req-999");
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("tenant", "acme");
        RequestContext requestContext = new RequestContext("test-schema", headers, queryParams, "/services/data");

        String result = VariableResolver.resolve(
                "token=!ref(sharedVariables.accessToken)&request_id=!ref(request.header.x-request-id)&tenant=!ref(request.query.tenant)",
                variables,
                requestContext
        );
        Assert.assertEquals("token=new-token&request_id=req-999&tenant=acme", result);
    }

    @Test
    public void testResolveRequestReferenceWithNullContext() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("accessToken", "my-token");

        // Request context is null, so request.header references should resolve to empty
        String result = VariableResolver.resolve(
                "token=!ref(sharedVariables.accessToken)&header=!ref(request.header.test)",
                variables,
                null
        );
        Assert.assertEquals("token=my-token&header=", result);
    }

    @Test
    public void testResolveRequestReferenceMissingHeader() {
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "token");
        RequestContext requestContext = new RequestContext("test-schema", headers, new HashMap<>(), "/api/test");

        // Reference a header that doesn't exist
        String result = VariableResolver.resolve(
                "Header: !ref(request.header.x-missing-header)",
                variables,
                requestContext
        );
        Assert.assertEquals("Header: ", result);
    }

    @Test
    public void testResolveMapWithRequestContext() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("clientId", "var-client");

        Map<String, String> headers = new HashMap<>();
        headers.put("x-correlation-id", "corr-abc");
        RequestContext requestContext = new RequestContext("test-schema", headers, new HashMap<>(), "/test");

        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("client_id", "!ref(sharedVariables.clientId)");
        templateMap.put("correlation_id", "!ref(request.header.x-correlation-id)");
        templateMap.put("static_value", "unchanged");

        Map<String, String> resolved = VariableResolver.resolveMap(templateMap, variables, requestContext);

        Assert.assertEquals("var-client", resolved.get("client_id"));
        Assert.assertEquals("corr-abc", resolved.get("correlation_id"));
        Assert.assertEquals("unchanged", resolved.get("static_value"));
    }

    @Test
    public void testResolveBearerToken() {
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature");
        RequestContext requestContext = new RequestContext("test-schema", headers, new HashMap<>(), "/api/test");

        // request.bearerToken should strip the "Bearer " prefix
        String result = VariableResolver.resolve(
                "subject_token=!ref(request.bearerToken)",
                variables,
                requestContext
        );
        Assert.assertEquals("subject_token=eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.signature", result);
    }

    @Test
    public void testResolveBearerTokenWithoutPrefix() {
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        // Authorization header without "Bearer " prefix (unusual but should still work)
        headers.put("authorization", "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.sig");
        RequestContext requestContext = new RequestContext("test-schema", headers, new HashMap<>(), "/api/test");

        String result = VariableResolver.resolve(
                "token=!ref(request.bearerToken)",
                variables,
                requestContext
        );
        Assert.assertEquals("token=eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ0ZXN0In0.sig", result);
    }

    @Test
    public void testResolveBearerTokenMissingHeader() {
        Map<String, Object> variables = new HashMap<>();
        Map<String, String> headers = new HashMap<>();
        // No Authorization header
        RequestContext requestContext = new RequestContext("test-schema", headers, new HashMap<>(), "/api/test");

        String result = VariableResolver.resolve(
                "token=!ref(request.bearerToken)",
                variables,
                requestContext
        );
        Assert.assertEquals("token=", result);
    }

    @Test
    public void testTokenExchangeScenario() {
        // Simulates a token exchange scenario where incoming JWT is used as subject_token
        Map<String, Object> variables = new HashMap<>();
        variables.put("clientId", "my-client");
        variables.put("clientSecret", "my-secret");

        Map<String, String> headers = new HashMap<>();
        headers.put("authorization", "Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIn0.sig");
        headers.put("x-correlation-id", "corr-12345");
        RequestContext requestContext = new RequestContext("test-schema", headers, new HashMap<>(), "/api/exchange");

        Map<String, String> templateMap = new HashMap<>();
        templateMap.put("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange");
        templateMap.put("client_id", "!ref(clientId)");
        templateMap.put("client_secret", "!ref(clientSecret)");
        templateMap.put("subject_token", "!ref(request.bearerToken)");
        templateMap.put("subject_token_type", "urn:ietf:params:oauth:token-type:access_token");

        Map<String, String> resolved = VariableResolver.resolveMap(templateMap, variables, requestContext);

        Assert.assertEquals("urn:ietf:params:oauth:grant-type:token-exchange", resolved.get("grant_type"));
        Assert.assertEquals("my-client", resolved.get("client_id"));
        Assert.assertEquals("my-secret", resolved.get("client_secret"));
        Assert.assertEquals("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ1c2VyMTIzIn0.sig", resolved.get("subject_token"));
        Assert.assertEquals("urn:ietf:params:oauth:token-type:access_token", resolved.get("subject_token_type"));
    }
}
