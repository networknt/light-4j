package com.networknt.client.simplepool.mockexample;

import com.networknt.client.simplepool.mock.TestRunner;

public class TestPoolSizeOverflow
{
    public static void main(String[] args) {
        new TestRunner()
            .setConnectionClass(MockKeepAliveConnection.class)
            .setExpireTime(5)
            .setCreateConnectionTimeout(5)
            .setHttp2(false)
            .setBorrowTime(2)
            .setBorrowTimeJitter(8)
            .setPoolSize(7)
            .setNumCallers(8)
            .setTestLength(10*60)
            .executeTest();
    }
}
