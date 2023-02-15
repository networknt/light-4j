package com.networknt.client.simplepool;

import java.net.URI;
import java.util.Set;

public interface SimpleConnectionMaker {
    public SimpleConnection makeConnection(long createConnectionTimeout, boolean isHttp2, final URI uri, final Set<SimpleConnection> allCreatedConnections);
    public SimpleConnection reuseConnection(long createConnectionTimeout, SimpleConnection connection) throws RuntimeException;
}
