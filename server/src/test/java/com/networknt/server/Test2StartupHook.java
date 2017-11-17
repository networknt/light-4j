package com.networknt.server;

public class Test2StartupHook implements StartupHookProvider {

    @Override
    public void onStartup() {
        System.out.println("Test2StartupHook is called");
    }
}
