package com.networknt.client.simplepool;

import java.net.URI;

public interface SimpleConnectionMaker {
    public SimpleConnection makeConnection(long createConnectionTimeout, boolean isHttp2, final URI uri) throws RuntimeException;
    public SimpleConnection reuseConnection(long createConnectionTimeout, SimpleConnection connection) throws RuntimeException;
}
