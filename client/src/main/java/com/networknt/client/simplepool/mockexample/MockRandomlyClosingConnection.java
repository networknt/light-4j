package com.networknt.client.simplepool.mockexample;

import com.networknt.client.simplepool.SimpleConnection;

import java.util.concurrent.ThreadLocalRandom;

public class MockRandomlyClosingConnection implements SimpleConnection {

    private volatile boolean closed = false;
    private boolean isHttp2 = true;
    private String MOCK_ADDRESS = "MOCK_HOST_IP:" + ThreadLocalRandom.current().nextInt((int) (Math.pow(2, 15) - 1.0), (int) (Math.pow(2, 16) - 1.0));

    /***
     * This mock connection simulates a multiplexable connection that has a 20% chance of taking longer than 5s
     * to be created
     */

    public MockRandomlyClosingConnection(boolean isHttp2) { this.isHttp2 = isHttp2; }

    @Override
    public boolean isOpen() {
        if(ThreadLocalRandom.current().nextInt(20) == 0)
            closed = true;
        return !closed;
    }

    @Override
    public Object getRawConnection() {
        throw new RuntimeException("Mock connection has no raw connection");
    }

    @Override
    public boolean isMultiplexingSupported() {
        return isHttp2;
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
