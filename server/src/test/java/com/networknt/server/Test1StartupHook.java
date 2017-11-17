package com.networknt.server;

public class Test1StartupHook implements StartupHookProvider {

    @Override
    public void onStartup() {
        System.out.println("Test1StartupHook is called");
    }
}
