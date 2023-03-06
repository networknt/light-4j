package com.networknt.client.simplepool.mockexample;

import com.networknt.client.simplepool.mock.TestRunner;

public class TestRandomlyClosingConnection
{
    public static void main(String[] args) {
        new TestRunner()
            .setConnectionClass(MockRandomlyClosingConnection.class)
            .setExpireTime(5)
            .setCreateConnectionTimeout(5)
            .setHttp2(false)
            .setBorrowTime(5)
            .setBorrowTimeJitter(5)
            .setNumCallers(8)
            .setTestLength(10*60)
            .executeTest();
    }
}