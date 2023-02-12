package com.networknt.client.simplepool.undertow;

import com.networknt.client.simplepool.SimpleConnection;
import io.undertow.client.ClientConnection;
import org.xnio.IoUtils;

public class SimpleClientConnection implements SimpleConnection {
    private ClientConnection connection;

    public SimpleClientConnection(ClientConnection connection) {
        this.connection = connection;
    }

    @Override
    public boolean isOpen() {
        return connection.isOpen();
    }

    @Override
    public Object getRawConnection() {
        return connection;
    }

    @Override
    public boolean isMultiplexingSupported() {
        return connection.isMultiplexingSupported();
    }

    @Override
    public String getLocalAddress() {
        return connection.getLocalAddress().toString();
    }

    @Override
    public void safeClose() {
        if(connection.isOpen())
            IoUtils.safeClose(connection);
    }
}
