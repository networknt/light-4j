package com.networknt.server;

public class Test1ShutdownHook implements ShutdownHookProvider {

    @Override
    public void onShutdown() {
        System.out.println("Test1ShutdownHook is called");
    }
}
