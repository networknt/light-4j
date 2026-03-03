package com.networknt.token.exchange.extract;

import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * Extracts client identity from JWT Bearer tokens.
 * Parses the JWT payload to find the client_id or subclaim.
 */
public class JwtClientIdentityExtractor implements ClientIdentity.Extractor {

    private static final Logger LOG = LoggerFactory.getLogger(JwtClientIdentityExtractor.class);
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public ClientIdentity extract(final String headerValue) {
        if (headerValue == null || headerValue.isEmpty()) {
            LOG.trace("Authorization header is empty or null");
            return null;
        }

        if (!headerValue.startsWith(BEARER_PREFIX)) {
            LOG.trace("Authorization header does not start with 'Bearer '");
            return null;
        }

        try {
            final var token = headerValue.substring(BEARER_PREFIX.length()).trim();
            final var parts = token.split("\\.");

            if (parts.length < 2) {
                LOG.trace("JWT does not have expected format (header.payload.signature)");
                return null;
            }

            // Decode the payload (second segment)
            final var payloadJson = new String(
                    Base64.getUrlDecoder().decode(parts[1]),
                    StandardCharsets.UTF_8
            );

            final var claims = JsonMapper.fromJson(payloadJson, Map.class);

            // Try client_id first, then fall back to sub (subject)
            String clientId = (String) claims.get("client_id");
            if (clientId == null) {
                clientId = (String) claims.get("sub");
            }

            if (clientId != null) {
                LOG.trace("Extracted client ID from JWT: {}", clientId);
                return new ClientIdentity(clientId, AuthType.JWT_BEARER);
            }
        } catch (Exception e) {
            LOG.warn("Failed to parse JWT token: {}", e.getMessage());
        }
        return null;
    }
}
