package com.networknt.client.simplepool.mockexample;

import com.networknt.client.simplepool.mock.TestRunner;

public class TestTimeoutLeakedConnection
{
    public static void main(String[] args) {
        new TestRunner()
            .setConnectionClass(MockTimeoutLeakedConnection.class)
            .setHttp2(false)
            .setNumCallers(8)
            .setTestLength(10*60)
            .executeTest();
    }
}
