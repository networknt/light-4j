package com.networknt.client.simplepool;

import java.net.URI;
import java.util.Set;

/***
 * A factory that creates raw connections and wraps them in SimpleConnection objects.
 * SimpleConnectionMakers are used by SimpleConnectionHolders to create connections.
 *
 */
public interface SimpleConnectionMaker {
    public SimpleConnection makeConnection(long createConnectionTimeout, boolean isHttp2, final URI uri, final Set<SimpleConnection> allCreatedConnections);
    public SimpleConnection reuseConnection(long createConnectionTimeout, SimpleConnection connection) throws RuntimeException;
}
