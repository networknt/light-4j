package com.networknt.server;

import com.networknt.config.Config;

import java.util.Map;

/**
 * This enum class is used to set and validate server options.
 * The following server options are supported:
 * 1. ioThreads
 * 2. workerThreads
 * 3. bufferSize
 * 4. serverString
 * 5. alwaysSetDate
 * 6. maxTransferFileSize
 * 7. allowUnescapedCharactersInUrl
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
    MAX_TRANSFER_FILE_SIZE("maxTransferFileSize"),
    ALLOW_UNESCAPED_CHARACTERS_IN_URL("allowUnescapedCharactersInUrl"),
    SHUTDOWN_TIMEOUT("shutdownTimeout");

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
                    ServerConfig.logger.warn("Server option: " + serverOption.value() + " set in server.yml is invalid, has been reset to default value.");
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
        return switch (serverOption) {
            case BACKLOG -> {
                if (value == null || Config.loadIntegerValue(ServerConfig.BACKLOG, value) <= 0) {
                    serverConfig.setBacklog(10000);
                    yield false;
                }
                yield true;
            }
            case IO_THREADS -> {
                if (value == null || Config.loadIntegerValue(ServerConfig.IO_THREADS, value) <= 0) {
                    serverConfig.setIoThreads(Runtime.getRuntime().availableProcessors() * 2);
                    yield false;
                }
                yield true;
            }
            case WORKER_THREADS -> {
                if (value == null || Config.loadIntegerValue(ServerConfig.WORKER_THREADS, value) <= 0) {
                    serverConfig.setWorkerThreads(200);
                    yield false;
                }
                yield true;
            }
            case BUFFER_SIZE -> {
                if (value == null || Config.loadIntegerValue(ServerConfig.BUFFER_SIZE, value) <= 0) {
                    serverConfig.setBufferSize(1024 * 16);
                    yield false;
                }
                yield true;
            }
            case SERVER_STRING -> {
                if (value == null || value.equals("")) {
                    serverConfig.setServerString("L");
                    yield false;
                }
                yield true;
            }
            case ALWAYS_SET_DATE -> {
                if (value == null) {
                    serverConfig.setAlwaysSetDate(true);
                }
                yield true;
            }
            case MAX_TRANSFER_FILE_SIZE -> {
                if (value == null || Config.loadLongValue(ServerConfig.MAX_TRANSFER_FILE_SIZE, value) <= 0) {
                    serverConfig.setMaxTransferFileSize(1000000);
                    yield false;
                }
                yield true;
            }
            case ALLOW_UNESCAPED_CHARACTERS_IN_URL -> {
                if (value == null) {
                    serverConfig.setAllowUnescapedCharactersInUrl(false);
                }
                yield true;
            }
            case SHUTDOWN_TIMEOUT -> {
                if (value == null || Config.loadIntegerValue(ServerConfig.SHUTDOWN_TIMEOUT, value) <= 0) {
                    serverConfig.setShutdownTimeout(null);
                    yield false;
                }
                yield true;
            }
            default -> true;
        };
    }

    private static void setToDefaultServerOption(ServerOption serverOption, ServerConfig serverConfig) {
        setServerOption(serverOption, null, serverConfig);
    }
}
