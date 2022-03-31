package com.networknt.limit.key;

import io.undertow.server.HttpServerExchange;

/**
 * When rate limit is used, we need to define a key to identify a unique client or a unique IP address.
 * The information can be from different places in the exchange, and we might need to combine several
 * strategies to get a unique key.
 *
 * @author Steve Hu
 */
public interface KeyResolver {
    /**
     * Resolve a unique key from the exchange
     * @param exchange server exchange
     * @return A string for the key
     */
    String resolve(HttpServerExchange exchange);
}
