package com.networknt.token.exchange.extract;

public interface ClientIdentityExtractor {
    ClientIdentity extract(final String headerValue);
}
