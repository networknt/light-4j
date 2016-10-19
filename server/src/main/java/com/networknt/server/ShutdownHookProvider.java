package com.networknt.server;

/**
 * If you want close database connections, release the resources allocated
 * in the application before server shutdown, please implement this interface
 * with a class and put it into your API project
 * /src/main/resources/META-INF/services/com.networknt.server.ShutdownHookProvider

 * Created by steve on 2016-10-19.
 */
public interface ShutdownHookProvider {
    void onShutdown();
}
