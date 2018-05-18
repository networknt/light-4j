package com.networknt.server;

/**
 * If you want close database connections, release the resource allocated
 * in the application before server shutdown, please implement this interface
 * with a class and put it into your API project
 * /src/main/resource/config/service.yml com.networknt.server.ShutdownHookProvider
 *
 * All shutdown hooks will be called during server shutdown so that resource can
 * be released completely.
 *
 * @author Steve Hu
 */
public interface ShutdownHookProvider {
    /**
     * Every implementation must implement this onShutdown method to hook in
     * some business logic during server shutdown phase.
     */
    void onShutdown();
}
