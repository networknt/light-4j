package com.networknt.server;

/**
 * If you want initialize database connections, load some resource or cached data
 * during server startup, please implement this interface with a class and put
 * it into your API project /src/main/resource/config/service.yml com.networknt.server.StartupHookProvider
 * During server startup, these startup hooks will be called to initialize the server
 * state.
 *
 * @author Steve Hu
 */
public interface StartupHookProvider {
    /**
     * Every implementation must implement this onStartup method to hook in
     * some business logic during server startup phase.
     */
    void onStartup();
}
