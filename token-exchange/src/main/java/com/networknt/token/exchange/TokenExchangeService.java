package com.networknt.token.exchange;


import com.networknt.token.exchange.schema.SharedVariableSchema;
import com.networknt.token.exchange.schema.TokenSchema;
import com.networknt.token.exchange.schema.UpdateSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
        final var keyStoreManager = new TokenKeyStoreManager();
        this.jwtBuilder = new JwtBuilder(keyStoreManager);
        this.httpClientFactory = new TokenHttpClientFactory(
                keyStoreManager,
                config.getProxyHost(),
                config.getProxyPort()
        );
    }

    public void transform(final RequestContext.Parser incomingContext) throws InterruptedException {
        var parsedContext = incomingContext.parseContext();
        if (parsedContext == null) {
            LOG.debug("Parser returned null context - skipping token transformation");
            return;
        }

        final var tokenSchema = this.config.getTokenSchemas().get(parsedContext.schemaKey());
        if (tokenSchema == null) {
            throw new IllegalArgumentException("Token schema '" + parsedContext.schemaKey() + "' does not exist");
        }
        final var sharedVariables = tokenSchema.getSharedVariables();
        if (isTokenExpired(tokenSchema)) {
            LOG.debug("Cached token is expired. Requesting a new token.");
            refreshTokenSafely(tokenSchema, sharedVariables, parsedContext);
        } else {
            LOG.debug("Using cached token.");
        }

        var update = tokenSchema.getTokenUpdate();
        var resultMap = this.createResultMap(update, sharedVariables, parsedContext);
        incomingContext.updateRequest(resultMap);
    }

    /**
     * Updates the result map with resolved token data.
     */
    private Map<String, Object> createResultMap(
            final UpdateSchema update,
            final SharedVariableSchema sharedVariables,
            final RequestContext requestContext
    ) {
        var resultMap = new HashMap<String, Object>();
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
        return resultMap;
    }

    /**
     * Thread-safe wrapper that ensures only one thread refreshes the token at a time.
     */
    private void refreshTokenSafely(
            final TokenSchema schema,
            final SharedVariableSchema sharedVariables,
            final RequestContext requestContext
    ) throws InterruptedException {
        final var lock = schema.getTokenRefreshLock();
        lock.lockInterruptibly();
        try {
            // Double-check if token is still expired after acquiring lock
            // Another thread may have already refreshed it
            if (isTokenExpired(schema)) {
                refreshToken(schema, sharedVariables, requestContext);
            } else {
                LOG.debug("Token was refreshed by another thread, using cached token.");
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * Refreshes the token by making a request to the token service.
     * This method should only be called within a lock to ensure thread safety.
     */
    private void refreshToken(
            final TokenSchema schema,
            final SharedVariableSchema sharedVariables,
            final RequestContext requestContext
    ) throws InterruptedException {
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
     * Checks if the cached token is expired (including grace period).
     */
    private boolean isTokenExpired(final TokenSchema schema) {
        final var sharedVariables = schema.getSharedVariables();
        final var waitLength = sharedVariables.getTokenTtlUnit().unitToMillis(sharedVariables.getWaitLength());
        return System.currentTimeMillis() >= (sharedVariables.getExpiration() - waitLength);
    }
}
