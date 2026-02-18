package com.networknt.token.exchange.extract;

public enum AuthType {
    BASIC,
    JWT_BEARER;

    public ClientIdentity.Extractor extractor() {
        return switch (this) {
            case BASIC -> new BasicAuthClientIdentityExtractor();
            case JWT_BEARER -> new JwtClientIdentityExtractor();
        };
    }
}
