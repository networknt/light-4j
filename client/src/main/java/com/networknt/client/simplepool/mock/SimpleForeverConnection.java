package com.networknt.client.simplepool.mock;

import com.networknt.client.simplepool.SimpleConnection;

import java.util.concurrent.ThreadLocalRandom;

public class SimpleForeverConnection implements SimpleConnection {

    private volatile boolean closed = false;
    private String MOCK_ADDRESS = "MOCK_HOST_IP:" + ThreadLocalRandom.current().nextInt((int) (Math.pow(2, 15) - 1.0), (int) (Math.pow(2, 16) - 1.0));

    /**
     * This mock connection simulates a multiplexable connection that never dies unless closed
     *
     * Simulates a server connection with indefinite keep-alives enabled
     *
     * @return
     */

    @Override
    public boolean isOpen() {
        return !closed;
    }

    @Override
    public Object getRawConnection() {
        throw new RuntimeException("Mock connection has no raw connection");
    }

    @Override
    public boolean isMultiplexingSupported() {
        return true;
    }

    @Override
    public String getLocalAddress() {
        return MOCK_ADDRESS;
    }

    @Override
    public void safeClose() {
        closed = true;
    }
}
