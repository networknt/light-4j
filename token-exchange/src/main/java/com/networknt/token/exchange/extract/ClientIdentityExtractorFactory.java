package com.networknt.token.exchange.extract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumMap;
import java.util.Map;

/**
 * Factory for creating and caching ClientIdentityExtractor instances based on AuthType.
 */
public class ClientIdentityExtractorFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ClientIdentityExtractorFactory.class);

    private static final Map<AuthType, ClientIdentityExtractor> EXTRACTORS = new EnumMap<>(AuthType.class);

    static {
        EXTRACTORS.put(AuthType.BASIC, new BasicAuthClientIdentityExtractor());
        EXTRACTORS.put(AuthType.JWT_BEARER, new JwtClientIdentityExtractor());
    }

    private ClientIdentityExtractorFactory() {
        // Utility class
    }

    /**
     * Gets the extractor for the specified auth type.
     *
     * @param authType the authentication type
     * @return the extractor, or null if no extractor is registered for the type
     */
    public static ClientIdentityExtractor getExtractor(final AuthType authType) {
        if (authType == null || authType == AuthType.UNKNOWN) {
            LOG.trace("No extractor for auth type: {}", authType);
            return null;
        }
        return EXTRACTORS.get(authType);
    }

    /**
     * Extracts client identity using the appropriate extractor for the auth type.
     *
     * @param authType the authentication type
     * @param headerValue the Authorization header value
     * @return the extracted ClientIdentity, or null if extraction failed
     */
    public static ClientIdentity extract(final AuthType authType, final String headerValue) {
        final var extractor = getExtractor(authType);
        if (extractor == null) {
            return null;
        }
        return extractor.extract(headerValue);
    }
}
