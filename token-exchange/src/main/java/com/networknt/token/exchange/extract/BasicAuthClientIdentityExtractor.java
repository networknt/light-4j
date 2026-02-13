package com.networknt.token.exchange.extract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Extracts client identity from Basic Authorization headers.
 * Format: "Basic base64(username:password)"
 */
public class BasicAuthClientIdentityExtractor implements ClientIdentityExtractor {

    private static final Logger LOG = LoggerFactory.getLogger(BasicAuthClientIdentityExtractor.class);
    private static final String BASIC_PREFIX = "Basic ";

    @Override
    public ClientIdentity extract(final String headerValue) {
        if (headerValue == null || headerValue.isEmpty()) {
            LOG.trace("Authorization header is empty or null");
            return null;
        }

        if (!headerValue.startsWith(BASIC_PREFIX)) {
            LOG.trace("Authorization header does not start with 'Basic '");
            return null;
        }

        try {
            final var encodedCredentials = headerValue.substring(BASIC_PREFIX.length()).trim();
            final var decoded = new String(Base64.getDecoder().decode(encodedCredentials), StandardCharsets.UTF_8);

            final int colonIndex = decoded.indexOf(':');
            if (colonIndex > 0) {
                final var username = decoded.substring(0, colonIndex);
                LOG.trace("Extracted client ID from Basic auth: {}", username);
                return new ClientIdentity(username, AuthType.BASIC);
            }
        } catch (IllegalArgumentException e) {
            LOG.warn("Failed to decode Basic auth header: {}", e.getMessage());
        }
        return null;
    }
}
