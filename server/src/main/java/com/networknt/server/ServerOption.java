package com.networknt.server;

import java.util.Map;

import static com.networknt.server.Server.logger;

/**
 * This enum class is used to set and validate server options.
 * The following server options are supported:
 * 1. ioThreads
 * 2. workerThreads
 * 3. bufferSize
 * 4. serverString
 * 5. alwaysSetDate
 * <p>
 * Note: To set these options, configuring them in server.yml
 */
public enum ServerOption {
    IO_THREADS("ioThreads"),
    WORKER_THREADS("workerThreads"),
    BUFFER_SIZE("bufferSize"),
    BACKLOG("backlog"),
    SERVER_STRING("serverString"),
    ALWAYS_SET_DATE("alwaysSetDate"),
    ALLOW_UNESCAPED_CHARACTERS_IN_URL("allowUnescapedCharactersInUrl");

    private final String value;

    ServerOption(String serverOption) {
        this.value = serverOption;
    }

    public String value() {
        return this.value;
    }

    /**
     * @param mapConfig    map config generated from server.yml
     * @param serverConfig object config generated from server.yml
     */
    protected static void serverOptionInit(Map<String, Object> mapConfig, ServerConfig serverConfig) {
        for (ServerOption serverOption : ServerOption.values()) {
            if (mapConfig.containsKey(serverOption.value())) {
                if (!setServerOption(serverOption, mapConfig.get(serverOption.value), serverConfig)) {
                    logger.warn("Server option: " + serverOption.value() + " set in server.yml is invalid, has been reset to default value.");
                }
            } else {
                setToDefaultServerOption(serverOption, serverConfig);
            }
        }
    }

    /**
     * Method used to validate and set server options
     *
     * @param serverOption the server option to be set
     * @param value        corresponding value for the server option
     * @param serverConfig object config generated from server.yml
     * @return validated result
     */
    private static boolean setServerOption(ServerOption serverOption, Object value, ServerConfig serverConfig) {
        switch (serverOption) {
            case BACKLOG:
                if (value == null || (int) value <= 0) {
                    serverConfig.setBacklog(10000);
                    return false;
                }
                return true;
            case IO_THREADS:
                if (value == null || (int) value <= 0) {
                    serverConfig.setIoThreads(Runtime.getRuntime().availableProcessors() * 2);
                    return false;
                }
                return true;
            case WORKER_THREADS:
                if (value == null || (int) value <= 0) {
                    serverConfig.setWorkerThreads(200);
                    return false;
                }
                return true;
            case BUFFER_SIZE:
                if (value == null || (int) value <= 0) {
                    serverConfig.setBufferSize(1024 * 16);
                    return false;
                }
                return true;
            case SERVER_STRING:
                if (value == null || value.equals("")) {
                    serverConfig.setServerString("L");
                    return false;
                }
                return true;
            case ALWAYS_SET_DATE:
                if (value == null) {
                    serverConfig.setAlwaysSetDate(true);
                }
                return true;
            case ALLOW_UNESCAPED_CHARACTERS_IN_URL:
                if (value == null) {
                    serverConfig.setAllowUnescapedCharactersInUrl(false);
                }
                return true;
            default:
                return true;
        }
    }

    private static void setToDefaultServerOption(ServerOption serverOption, ServerConfig serverConfig) {
        setServerOption(serverOption, null, serverConfig);
    }
}
