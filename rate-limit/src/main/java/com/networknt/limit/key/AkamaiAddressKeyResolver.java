package com.networknt.limit.key;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;

/**
 * When light-gateway is used for external clients and all external requests go through the
 * Akamai cloud proxy, the real client IP can be retrieved from the header as True-Client-IP
 *
 * @author Steve Hu
 */
public class AkamaiAddressKeyResolver implements KeyResolver {

    @Override
    public String resolve(HttpServerExchange exchange) {
        String key = "127.0.0.1";
        HeaderMap headerMap = exchange.getResponseHeaders();
        HeaderValues values = headerMap.get("True-Client-IP");
        if(values != null) key = values.getFirst();
        return key;
    }
}
