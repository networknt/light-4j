package com.networknt.token.exchange;

import com.networknt.token.exchange.extract.ClientIdentityExtractorFactory;
import com.networknt.token.exchange.schema.SharedVariableSchema;
import com.networknt.token.exchange.schema.TokenSchema;
import com.networknt.token.exchange.schema.UpdateSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Core service for token transformation operations.
 * This service can be used independently of the rule engine plugin.
 *
 * <p>Use cases:</p>
 * <ul>
 *   <li>Direct schema resolution by name (for rule engine plugin)</li>
 *   <li>Schema resolution based on client ID from Authorization header (for handlers)</li>
 *   <li>Path-based authentication type resolution using pathAuthMappings configuration</li>
 * </ul>
 */
public class TokenExchangeService {

    private static final Logger LOG = LoggerFactory.getLogger(TokenExchangeService.class);

    public static final String REQUEST_HEADERS = "requestHeaders";
    public static final String UPDATE = "update";

    private final TokenExchangeConfig config;
    private final JwtBuilder jwtBuilder;
    private final TokenHttpClientFactory httpClientFactory;

    /**
     * Creates a new TokenTransformationService with the default configuration.
     */
    public TokenExchangeService() {
        this(TokenExchangeConfig.load());
    }

    /**
     * Creates a new TokenTransformationService with the provided configuration.
     *
     * @param config the token transformer configuration
     */
    public TokenExchangeService(final TokenExchangeConfig config) {
        this.config = config;
        TokenKeyStoreManager keyStoreManager = new TokenKeyStoreManager();
        this.jwtBuilder = new JwtBuilder(keyStoreManager);
        this.httpClientFactory = new TokenHttpClientFactory(
                keyStoreManager,
                config.getProxyHost(),
                config.getProxyPort()
        );
    }

    /**
     * Gets the configuration used by this service.
     */
    public TokenExchangeConfig getConfig() {
        return config;
    }

    /**
     * Transforms a request using the specified token schema name.
     * This is the primary method for use case 1 (rule engine plugin).
     *
     * @param tokenSchemaName the name of the token schema to use
     * @param resultMap the result map to populate with token data
     * @throws InterruptedException if the token request is interrupted
     * @throws IllegalArgumentException if the schema doesn't exist
     */
    public void transformBySchemaName(final String tokenSchemaName, final Map<String, Object> resultMap)
            throws InterruptedException {
        transformBySchemaName(tokenSchemaName, resultMap, null);
    }

    /**
     * Transforms a request using the specified token schema name with request context.
     * This allows shared variables to reference request headers and query parameters.
     *
     * @param tokenSchemaName the name of the token schema to use
     * @param resultMap the result map to populate with token data
     * @param requestContext the request context containing headers and query parameters (may be null)
     * @throws InterruptedException if the token request is interrupted
     * @throws IllegalArgumentException if the schema doesn't exist
     */
    public void transformBySchemaName(final String tokenSchemaName, final Map<String, Object> resultMap,
                                       final RequestContext requestContext)
            throws InterruptedException {
        if (config.getTokenSchemas() == null) {
            LOG.warn("No token schemas configured");
            return;
        }

        final var schema = config.getTokenSchemas().get(tokenSchemaName);
        if (schema == null) {
            throw new IllegalArgumentException("Token schema '" + tokenSchemaName + "' does not exist");
        }

        transformWithSchema(schema, resultMap, requestContext);
    }

    /**
     * Transforms a request by resolving the schema from a client ID.
     *
     * @param clientId the client ID to resolve the schema from
     * @param resultMap the result map to populate with token data
     * @param requestContext the request context containing headers and query parameters (may be null)
     * @return true if transformation was successful, false if no mapping found
     * @throws InterruptedException if the token request is interrupted
     */
    public boolean transformByClientId(
            final String clientId,
            final Map<String, Object> resultMap,
            final RequestContext requestContext
    ) throws InterruptedException {
        final var schemaName = config.resolveSchemaFromClientId(clientId);
        if (schemaName == null) {
            LOG.debug("No schema mapping found for client '{}'", clientId);
            return false;
        }

        LOG.debug("Resolved schema '{}' for client '{}'", schemaName, clientId);
        transformBySchemaName(schemaName, resultMap, requestContext);
        return true;
    }

    /**
     * Transforms a request using the provided RequestContext.
     * Extracts the authorization header and path from the context, resolves the auth type
     * from the path, extracts client identity, and performs the transformation.
     *
     * @param requestContext the request context containing headers and path
     * @param resultMap the result map to populate with token data
     * @return true if transformation was successful, false if no mapping found
     * @throws InterruptedException if the token request is interrupted
     */
    public boolean transformByRequestContext(
            final RequestContext requestContext,
            final Map<String, Object> resultMap
    ) throws InterruptedException {
        if (requestContext == null) {
            LOG.debug("Request context is null");
            return false;
        }

        final var authHeader = requestContext.getAuthorizationHeader();
        if (authHeader == null || authHeader.isEmpty()) {
            LOG.debug("Authorization header is empty");
            return false;
        }

        // Resolve auth type from path
        final var path = requestContext.getPath();
        final var authType = config.resolveAuthTypeFromPath(path);
        if (authType == null) {
            LOG.debug("No auth type configured for path '{}', cannot extract client identity", path);
            return false;
        }

        LOG.debug("Resolved auth type '{}' for path '{}'", authType, path);

        // Extract client identity using the resolved auth type
        final var clientIdentity = ClientIdentityExtractorFactory.extract(authType, authHeader);
        if (clientIdentity == null) {
            LOG.debug("Could not extract client identity from Authorization header using auth type {}", authType);
            return false;
        }

        return transformByClientId(clientIdentity.id(), resultMap, requestContext);
    }


    /**
     * Gets the token schema by name.
     *
     * @param schemaName the schema name
     * @return the token schema, or null if not found
     */
    public TokenSchema getSchema(final String schemaName) {
        if (config.getTokenSchemas() == null) {
            return null;
        }
        return config.getTokenSchemas().get(schemaName);
    }

    /**
     * Resolves schema name from client ID using clientMappings.
     *
     * @param clientId the client ID
     * @return the schema name, or null if no mapping exists
     */
    public String resolveSchemaName(final String clientId) {
        return config.resolveSchemaFromClientId(clientId);
    }

    /**
     * Performs the token transformation using the provided schema.
     * Synchronized on the schema to prevent race conditions.
     */
    private void transformWithSchema(final TokenSchema schema, final Map<String, Object> resultMap,
                                     final RequestContext requestContext)
            throws InterruptedException {
        synchronized (schema) {
            final var sharedVariables = schema.getSharedVariables();

            if (isTokenExpired(schema)) {
                LOG.debug("Cached token is expired. Requesting a new token.");
                refreshToken(schema, sharedVariables, requestContext);
            } else {
                LOG.debug("Using cached token.");
            }

            updateResultMap(schema.getTokenUpdate(), sharedVariables, resultMap, requestContext);
        }
    }

    /**
     * Refreshes the token by making a request to the token service.
     */
    private void refreshToken(final TokenSchema schema, final SharedVariableSchema sharedVariables,
                              final RequestContext requestContext)
            throws InterruptedException {
        final var requestSchema = schema.getTokenRequest();

        // Build JWT if required
        if (requestSchema.getJwtSchema() != null) {
            final var jwt = this.jwtBuilder.build(requestSchema.getJwtSchema());
            sharedVariables.set(SharedVariableSchema.CONSTRUCTED_JWT, jwt);
            LOG.trace("Constructed JWT for token request");
        }

        // Prepare and send request
        final var resolvedHeaders = requestSchema.getResolvedHeaders(sharedVariables, requestContext);
        final var resolvedBody = requestSchema.getResolvedBody(sharedVariables, requestContext);

        final var client = this.httpClientFactory.getOrCreateClient(requestSchema);
        final var request = this.httpClientFactory.buildRequest(requestSchema, resolvedHeaders, resolvedBody);
        final var response = this.httpClientFactory.send(client, request);

        processTokenResponse(response, schema, sharedVariables);
    }

    /**
     * Processes the token response and updates shared variables.
     */
    private void processTokenResponse(
            final HttpResponse<String> response,
            final TokenSchema schema,
            final SharedVariableSchema sharedVariables
    ) {
        if (response.statusCode() < 200 || response.statusCode() > 299) {
            LOG.error("Token request returned status code: {}", response.statusCode());
            return;
        }

        // Write response data to shared variables
        schema.getTokenSource().writeResponseToSharedVariables(sharedVariables, response);

        // Update expiration if configured
        if (schema.getTokenUpdate().isUpdateExpirationFromTtl()) {
            sharedVariables.updateExpiration();
        }
    }

    /**
     * Updates the result map with resolved token data.
     */
    private void updateResultMap(
            final UpdateSchema update,
            final SharedVariableSchema sharedVariables,
            final Map<String, Object> resultMap,
            final RequestContext requestContext
    ) {
        // Update body if configured
        if (update.getBody() != null && !update.getBody().isEmpty()) {
            resultMap.put("requestBody", new HashMap<>(update.getResolvedBody(sharedVariables, requestContext)));
        }

        // Update headers if configured
        if (update.getHeaders() != null && !update.getHeaders().isEmpty()) {
            @SuppressWarnings("unchecked")
            var requestHeaders = (Map<String, Object>) resultMap.get(REQUEST_HEADERS);
            if (requestHeaders == null) {
                requestHeaders = new HashMap<>();
                resultMap.put(REQUEST_HEADERS, requestHeaders);
            }

            @SuppressWarnings("unchecked")
            var updateMap = (Map<String, String>) requestHeaders.get(UPDATE);
            if (updateMap == null) {
                updateMap = new HashMap<>();
                requestHeaders.put(UPDATE, updateMap);
            }
            updateMap.putAll(update.getResolvedHeaders(sharedVariables, requestContext));
        }
    }

    /**
     * Checks if the cached token is expired (including grace period).
     */
    private boolean isTokenExpired(final TokenSchema schema) {
        final var sharedVariables = schema.getSharedVariables();
        final var waitLength = sharedVariables.getTokenTtlUnit().unitToMillis(sharedVariables.getWaitLength());
        return System.currentTimeMillis() >= (sharedVariables.getExpiration() - waitLength);
    }
}
