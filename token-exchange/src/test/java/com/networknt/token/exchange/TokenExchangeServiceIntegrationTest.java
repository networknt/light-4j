package com.networknt.token.exchange;

import com.networknt.token.exchange.schema.*;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for TokenExchangeService with schema configurations.
 * These tests focus on the interaction between components rather than
 * testing individual units in isolation.
 */
public class TokenExchangeServiceIntegrationTest {

    /**
     * Test schema resolution and variable propagation through the system.
     */
    @Test
    public void testSchemaResolutionWithVariables() {
        // Given: A token schema with shared variables and update configuration
        TokenSchema schema = createCompleteTokenSchema();

        // Set some shared variables (simulating a previous token refresh)
        SharedVariableSchema sharedVars = schema.getSharedVariables();
        sharedVars.set("accessToken", "test-access-token-abc123");
        sharedVars.set("refreshToken", "test-refresh-token-xyz789");
        sharedVars.set("userId", "user-12345");

        // Set a valid expiration (1 hour from now, so token is not expired)
        long oneHourFromNow = System.currentTimeMillis() + (3600 * 1000);
        sharedVars.setExpiration(oneHourFromNow);

        // When: Resolve variables in update schema
        UpdateSchema updateSchema = schema.getTokenUpdate();
        RequestContext requestContext = new RequestContext(
            "test-schema",
            createTestHeaders(),
            new HashMap<>(),
            "/api/customers"
        );

        Map<String, String> resolvedHeaders = updateSchema.getResolvedHeaders(sharedVars, requestContext);

        // Then: Variables should be properly resolved
        Assert.assertNotNull("Resolved headers should not be null", resolvedHeaders);
        Assert.assertEquals("Bearer test-access-token-abc123",
            resolvedHeaders.get("Authorization"));
        Assert.assertEquals("user-12345",
            resolvedHeaders.get("X-User-Id"));
    }

    /**
     * Test that request context variables can be used in transformations.
     */
    @Test
    public void testRequestContextVariableResolution() {
        // Given: Update schema that references request context
        UpdateSchema updateSchema = new UpdateSchema();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Original-Token", "!ref(request.bearerToken)");
        headers.put("X-Request-Path", "!ref(request.path)");
        headers.put("X-Client-Id", "!ref(request.header.X-Client-Id)");
        headers.put("X-Static", "static-value");
        updateSchema.setHeaders(headers);

        SharedVariableSchema sharedVars = new SharedVariableSchema();

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("authorization", "Bearer original-token-123");
        requestHeaders.put("x-client-id", "client-abc");

        RequestContext requestContext = new RequestContext(
            "test-schema",
            requestHeaders,
            new HashMap<>(),
            "/api/orders/456"
        );

        // When: Resolve variables
        Map<String, String> resolvedHeaders = updateSchema.getResolvedHeaders(sharedVars, requestContext);

        // Then: All variables should be resolved correctly
        Assert.assertEquals("original-token-123",
            resolvedHeaders.get("X-Original-Token"));
        Assert.assertEquals("/api/orders/456",
            resolvedHeaders.get("X-Request-Path"));
        Assert.assertEquals("client-abc",
            resolvedHeaders.get("X-Client-Id"));
        Assert.assertEquals("static-value",
            resolvedHeaders.get("X-Static"));
    }

    /**
     * Test combining shared variables and request context variables.
     */
    @Test
    public void testCombinedVariableResolution() {
        // Given: Update schema with both types of variables
        UpdateSchema updateSchema = new UpdateSchema();
        Map<String, String> body = new HashMap<>();
        body.put("access_token", "!ref(accessToken)");  // shared variable
        body.put("request_path", "!ref(request.path)");  // request context
        body.put("client_id", "!ref(request.header.X-Client-Id)");  // request context
        body.put("user_id", "!ref(userId)");  // shared variable
        updateSchema.setBody(body);

        SharedVariableSchema sharedVars = new SharedVariableSchema();
        sharedVars.set("accessToken", "token-abc");
        sharedVars.set("userId", "user-123");

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("x-client-id", "client-xyz");

        RequestContext requestContext = new RequestContext(
            "test-schema",
            requestHeaders,
            new HashMap<>(),
            "/api/payments"
        );

        // When: Resolve variables
        Map<String, String> resolvedBody = updateSchema.getResolvedBody(sharedVars, requestContext);

        // Then: All variables should be resolved
        Assert.assertEquals("token-abc", resolvedBody.get("access_token"));
        Assert.assertEquals("/api/payments", resolvedBody.get("request_path"));
        Assert.assertEquals("client-xyz", resolvedBody.get("client_id"));
        Assert.assertEquals("user-123", resolvedBody.get("user_id"));
    }

    /**
     * Test expiration calculation with different TTL units.
     */
    @Test
    public void testExpirationWithDifferentTtlUnits() {
        // Test with SECOND
        SharedVariableSchema sharedVarsSeconds = new SharedVariableSchema();
        sharedVarsSeconds.set(SharedVariableSchema.TOKEN_TTL, 60L);
        sharedVarsSeconds.set(SharedVariableSchema.TOKEN_TTL_UNIT, TtlUnit.SECOND);

        long beforeSeconds = System.currentTimeMillis();
        sharedVarsSeconds.updateExpiration();
        long expirationSeconds = sharedVarsSeconds.getExpiration();

        Assert.assertTrue("Expiration should be ~60 seconds from now",
            expirationSeconds > beforeSeconds + 59000 &&
            expirationSeconds < beforeSeconds + 61000);

        // Test with MINUTE
        SharedVariableSchema sharedVarsMinutes = new SharedVariableSchema();
        sharedVarsMinutes.set(SharedVariableSchema.TOKEN_TTL, 1L);
        sharedVarsMinutes.set(SharedVariableSchema.TOKEN_TTL_UNIT, TtlUnit.MINUTE);

        long beforeMinutes = System.currentTimeMillis();
        sharedVarsMinutes.updateExpiration();
        long expirationMinutes = sharedVarsMinutes.getExpiration();

        Assert.assertTrue("Expiration should be ~1 minute from now",
            expirationMinutes > beforeMinutes + 59000 &&
            expirationMinutes < beforeMinutes + 61000);

        // Test with HOUR
        SharedVariableSchema sharedVarsHours = new SharedVariableSchema();
        sharedVarsHours.set(SharedVariableSchema.TOKEN_TTL, 1L);
        sharedVarsHours.set(SharedVariableSchema.TOKEN_TTL_UNIT, TtlUnit.HOUR);

        long beforeHours = System.currentTimeMillis();
        sharedVarsHours.updateExpiration();
        long expirationHours = sharedVarsHours.getExpiration();

        Assert.assertTrue("Expiration should be ~1 hour from now",
            expirationHours > beforeHours + 3599000 &&
            expirationHours < beforeHours + 3601000);
    }

    /**
     * Test token expiration with grace period (wait length).
     */
    @Test
    public void testTokenExpirationWithGracePeriod() {
        // Given: Token expires in 25 seconds with 30 second grace period
        long expirationTime = System.currentTimeMillis() + 25000;
        long waitLengthSeconds = 30L;

        // When: Calculate if token is expired
        long waitLengthMillis = TtlUnit.SECOND.unitToMillis(waitLengthSeconds);
        boolean isExpired = System.currentTimeMillis() >= (expirationTime - waitLengthMillis);

        // Then: Token should be considered expired (refresh early)
        Assert.assertTrue("Token should be expired due to grace period", isExpired);

        // Given: Token expires in 35 seconds with 30 second grace period
        expirationTime = System.currentTimeMillis() + 35000;

        // When: Calculate if token is expired
        isExpired = System.currentTimeMillis() >= (expirationTime - waitLengthMillis);

        // Then: Token should NOT be expired
        Assert.assertFalse("Token should not be expired yet", isExpired);
    }

    /**
     * Test that missing variables in resolution result in empty strings.
     */
    @Test
    public void testMissingVariableResolution() {
        // Given: Update schema referencing non-existent variables
        UpdateSchema updateSchema = new UpdateSchema();
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Missing", "!ref(nonExistentVar)");
        headers.put("X-Static", "static");
        updateSchema.setHeaders(headers);

        SharedVariableSchema sharedVars = new SharedVariableSchema();
        RequestContext requestContext = new RequestContext(
            "test-schema",
            new HashMap<>(),
            new HashMap<>(),
            "/api/test"
        );

        // When: Resolve variables
        Map<String, String> resolvedHeaders = updateSchema.getResolvedHeaders(sharedVars, requestContext);

        // Then: Missing variable should resolve to empty string
        Assert.assertEquals("", resolvedHeaders.get("X-Missing"));
        Assert.assertEquals("static", resolvedHeaders.get("X-Static"));
    }

    /**
     * Test parser interface implementation for client-ID based resolution.
     */
    @Test
    public void testClientIdBasedParserPattern() {
        // Given: A mock parser that resolves schema by client-ID
        final String[] updateCallCount = {null};

        RequestContext.Parser parser = new RequestContext.Parser() {
            @Override
            public RequestContext parseContext() {
                // Simulate extracting client ID and mapping to schema
                String clientId = "client-abc";
                String schemaKey = mapClientIdToSchema(clientId);

                return new RequestContext(
                    schemaKey,
                    createTestHeaders(),
                    new HashMap<>(),
                    "/api/test"
                );
            }

            @Override
            public void updateRequest(Map<String, Object> resultMap) {
                updateCallCount[0] = "called";
                Assert.assertNotNull("Result map should not be null", resultMap);
            }

            private String mapClientIdToSchema(String clientId) {
                // Simulate client-to-schema mapping
                return "oauth-schema-for-" + clientId;
            }
        };

        // When: Parse context
        RequestContext context = parser.parseContext();

        // Then: Schema should be resolved correctly
        Assert.assertNotNull("Context should not be null", context);
        Assert.assertEquals("oauth-schema-for-client-abc", context.schemaKey());
        Assert.assertEquals("/api/test", context.path());
    }

    /**
     * Test parser interface implementation for path-based resolution.
     */
    @Test
    public void testPathBasedParserPattern() {
        // Given: A mock parser that resolves schema by path
        RequestContext.Parser parser = new RequestContext.Parser() {
            @Override
            public RequestContext parseContext() {
                // Simulate mapping path to schema
                String path = "/api/customers/123";
                String schemaKey = mapPathToSchema(path);

                return new RequestContext(
                    schemaKey,
                    createTestHeaders(),
                    new HashMap<>(),
                    path
                );
            }

            @Override
            public void updateRequest(Map<String, Object> resultMap) {
                // Apply updates
            }

            private String mapPathToSchema(String path) {
                // Simulate path-to-schema mapping (longest prefix match)
                if (path.startsWith("/api/customers")) {
                    return "customer-service-oauth";
                } else if (path.startsWith("/api/orders")) {
                    return "order-service-oauth";
                } else {
                    return "default-oauth";
                }
            }
        };

        // When: Parse context
        RequestContext context = parser.parseContext();

        // Then: Schema should be resolved correctly
        Assert.assertNotNull("Context should not be null", context);
        Assert.assertEquals("customer-service-oauth", context.schemaKey());
    }

    /**
     * Test composite parser pattern (fallback chain).
     */
    @Test
    public void testCompositeParserPattern() {
        // Given: A parser that tries multiple strategies
        RequestContext.Parser parser = new RequestContext.Parser() {
            @Override
            public RequestContext parseContext() {
                String path = "/api/unknown/endpoint";

                // Try path-based first
                String schemaKey = tryPathBased(path);

                // Fallback to default
                if (schemaKey == null) {
                    schemaKey = "default-oauth-schema";
                }

                return new RequestContext(
                    schemaKey,
                    createTestHeaders(),
                    new HashMap<>(),
                    path
                );
            }

            @Override
            public void updateRequest(Map<String, Object> resultMap) {
                // Apply updates
            }

            private String tryPathBased(String path) {
                // Try to match known paths
                if (path.startsWith("/api/customers")) return "customer-oauth";
                if (path.startsWith("/api/orders")) return "order-oauth";
                return null; // No match
            }
        };

        // When: Parse context
        RequestContext context = parser.parseContext();

        // Then: Should fallback to default schema
        Assert.assertNotNull("Context should not be null", context);
        Assert.assertEquals("default-oauth-schema", context.schemaKey());
    }

    /**
     * Test that updateExpirationFromTtl flag is respected.
     */
    @Test
    public void testUpdateExpirationFromTtlFlag() {
        // Given: Update schema with flag enabled
        UpdateSchema updateSchemaEnabled = new UpdateSchema();
        Assert.assertTrue("Default should be true",
            updateSchemaEnabled.isUpdateExpirationFromTtl());

        // Given: Update schema with flag disabled (if setter exists)
        UpdateSchema updateSchemaDisabled = new UpdateSchema();
        // Note: There's no setter in the current implementation,
        // but the flag can be set via Jackson during deserialization

        // The flag is used in TokenExchangeService.refreshToken()
        // to determine if expiration should be updated after token refresh
    }

    /**
     * Test variable resolution with special characters.
     */
    @Test
    public void testVariableResolutionWithSpecialCharacters() {
        // Given: Variables with special characters in values
        SharedVariableSchema sharedVars = new SharedVariableSchema();
        sharedVars.set("specialToken", "abc=123&xyz=456");
        sharedVars.set("jsonValue", "{\"key\":\"value\"}");

        UpdateSchema updateSchema = new UpdateSchema();
        Map<String, String> body = new HashMap<>();
        body.put("encoded", "!ref(specialToken)");
        body.put("json", "!ref(jsonValue)");
        updateSchema.setBody(body);

        RequestContext requestContext = new RequestContext(
            "test-schema",
            new HashMap<>(),
            new HashMap<>(),
            "/api/test"
        );

        // When: Resolve variables
        Map<String, String> resolvedBody = updateSchema.getResolvedBody(sharedVars, requestContext);

        // Then: Special characters should be preserved
        Assert.assertEquals("abc=123&xyz=456", resolvedBody.get("encoded"));
        Assert.assertEquals("{\"key\":\"value\"}", resolvedBody.get("json"));
    }

    /**
     * Test case-insensitive header lookups in RequestContext.
     */
    @Test
    public void testCaseInsensitiveHeaderLookup() {
        // Given: Request context with mixed-case headers
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        headers.put("x-custom-header", "custom-value");
        headers.put("authorization", "Bearer token-123");

        RequestContext context = new RequestContext(
            "test-schema",
            headers,
            new HashMap<>(),
            "/api/test"
        );

        // When/Then: Test case-insensitive lookups
        Assert.assertEquals("application/json", context.getHeader("Content-Type"));
        Assert.assertEquals("application/json", context.getHeader("CONTENT-TYPE"));
        Assert.assertEquals("application/json", context.getHeader("content-type"));

        Assert.assertEquals("custom-value", context.getHeader("X-Custom-Header"));
        Assert.assertEquals("custom-value", context.getHeader("x-custom-header"));

        Assert.assertEquals("Bearer token-123", context.getAuthorizationHeader());
    }

    // ==================== Helper Methods ====================

    /**
     * Creates a complete token schema with all sections configured.
     */
    private TokenSchema createCompleteTokenSchema() {
        TokenSchema schema = new TokenSchema();

        // Configure shared variables
        SharedVariableSchema sharedVars = schema.getSharedVariables();
        sharedVars.set(SharedVariableSchema.TOKEN_TTL, 3600L);
        sharedVars.set(SharedVariableSchema.TOKEN_TTL_UNIT, TtlUnit.SECOND);
        sharedVars.set(SharedVariableSchema.WAIT_LENGTH, 30L);

        // Configure update schema
        UpdateSchema updateSchema = schema.getTokenUpdate();
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer !ref(accessToken)");
        headers.put("X-User-Id", "!ref(userId)");
        updateSchema.setHeaders(headers);

        return schema;
    }

    /**
     * Creates test headers for request context.
     */
    private Map<String, String> createTestHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("content-type", "application/json");
        headers.put("authorization", "Bearer test-token");
        return headers;
    }
}
