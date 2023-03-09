package com.networknt.client.simplepool.mock.mockexample;

import com.networknt.client.simplepool.SimpleConnection;
import java.util.concurrent.ThreadLocalRandom;

public class MockTimeoutLeakedConnection implements SimpleConnection {

    private volatile boolean closed = false;
    private boolean isHttp2 = true;
    private String MOCK_ADDRESS = "MOCK_HOST_IP:" + ThreadLocalRandom.current().nextInt((int) (Math.pow(2, 15) - 1.0), (int) (Math.pow(2, 16) - 1.0));

    /***
     * This mock connection simulates a multiplexable connection that has a 20% chance of taking longer than 5s
     * to be created
     */

    public MockTimeoutLeakedConnection(boolean isHttp2) {
        this.isHttp2 = isHttp2;
        randomCreationDelay();
    }

    private void randomCreationDelay() throws RuntimeException {
        if(ThreadLocalRandom.current().nextInt(5) == 0) {
            try {
                Thread.sleep(10*1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

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
