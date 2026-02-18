package com.networknt.token.exchange.extract;

public record ClientIdentity(String id, AuthType type) {
    public interface Extractor {
        ClientIdentity extract(final String headerValue);
    }
}
