package com.networknt.client.simplepool.mock;

import com.networknt.client.simplepool.SimpleConnection;
import com.networknt.client.simplepool.SimpleConnectionMaker;

import java.net.URI;
import java.util.Set;

public class SimpleForeverConnectionMaker implements SimpleConnectionMaker {
    @Override
    public SimpleConnection makeConnection(long createConnectionTimeout, boolean isHttp2, URI uri, Set allConnections)
        throws RuntimeException
    {
        SimpleConnection connection = new SimpleForeverConnection();
        allConnections.add(connection);
        return connection;
    }

    @Override
    public SimpleConnection reuseConnection(long createConnectionTimeout, SimpleConnection connection) throws RuntimeException {
        if(connection.isOpen())
            return connection;
        throw new RuntimeException("Cannot reuse closed mock connection");
    }
}
