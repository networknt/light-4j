package com.networknt.token.exchange.extract;

public enum AuthType {
    BASIC,
    JWT_BEARER;

    private static final ClientIdentity.Extractor BASIC_EXTRACTOR = new BasicAuthClientIdentityExtractor();
    private static final ClientIdentity.Extractor JWT_BEARER_EXTRACTOR = new JwtClientIdentityExtractor();

    public ClientIdentity.Extractor extractor() {
        return switch (this) {
            case BASIC -> BASIC_EXTRACTOR;
            case JWT_BEARER -> JWT_BEARER_EXTRACTOR;
        };
    }
}
