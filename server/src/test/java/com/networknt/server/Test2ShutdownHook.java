package com.networknt.server;

public class Test2ShutdownHook implements ShutdownHookProvider {

    @Override
    public void onShutdown() {
        System.out.println("Test2ShutdownHook is called");
    }
}
