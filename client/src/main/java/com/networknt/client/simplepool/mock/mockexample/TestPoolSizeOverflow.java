package com.networknt.client.simplepool.mock.mockexample;

import com.networknt.client.simplepool.mock.TestRunner;

public class TestPoolSizeOverflow
{
    public static void main(String[] args) {
        new TestRunner()
            // set connection properties
            .setConnectionPoolSize(7)
            .setSimpleConnectionClass(MockKeepAliveConnection.class)
            .setCreateConnectionTimeout(5)
            .setConnectionExpireTime(5)
            .setHttp2(false)

            // configure borrower-thread properties
            .setNumBorrowerThreads(8)
            .setBorrowerThreadStartJitter(0)
            .setBorrowTimeLength(2)
            .setBorrowTimeLengthJitter(8)
            .setWaitTimeBeforeReborrow(1)
            .setWaitTimeBeforeReborrowJitter(1)

            .setTestLength(10*60)
            .executeTest();
    }
}
