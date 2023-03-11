package com.networknt.client.simplepool.mock.mockexample;

import com.networknt.client.simplepool.mock.TestRunner;

public class TestKeepAliveConnection
{
    public static void main(String[] args) {
        new TestRunner()
            // set connection properties
            .setConnectionPoolSize(100)
            .setSimpleConnectionClass(MockKeepAliveConnection.class)
            .setCreateConnectionTimeout(5)
            .setConnectionExpireTime(5)
            .setHttp2(false)

            // configure borrower-thread properties
            .setNumBorrowerThreads(4)
            .setBorrowerThreadStartJitter(0)
            .setBorrowTimeLength(5)
            .setBorrowTimeLengthJitter(5)
            .setWaitTimeBeforeReborrow(2)
            .setWaitTimeBeforeReborrowJitter(2)

            .setTestLength(10*60)
            .executeTest();
    }
}
