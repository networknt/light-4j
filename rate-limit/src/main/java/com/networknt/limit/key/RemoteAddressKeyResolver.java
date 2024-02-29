package com.networknt.limit.key;

import io.undertow.server.HttpServerExchange;

import java.net.InetSocketAddress;

/**
 * When address is used as the key, we can get the IP address from the header of the request. If there
 * is no proxy before our service and gateway, we can use the remote address for the purpose.
 *
 * @author Steve Hu
 */
public class RemoteAddressKeyResolver implements KeyResolver {

    @Override
    public String resolve(HttpServerExchange exchange) {
        InetSocketAddress address = exchange.getSourceAddress();
        return address.getHostString();
    }
}
