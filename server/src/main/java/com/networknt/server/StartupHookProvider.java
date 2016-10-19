package com.networknt.server;

/**
 * If you want initialize database connections, load Spring application context
 * during server startup, please implement this interface with a class and put
 * it into your API project /src/main/resources/META-INF/services/com.networknt.server.StartupHookProvider
 *
 * Created by steve on 2016-10-19.
 */
public interface StartupHookProvider {
    void onStartup();
}
