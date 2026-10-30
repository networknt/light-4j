package com.networknt.server;

import com.networknt.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

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
    IO_THREADS("ioThreads", ServerConfig::getIoThreads),
    WORKER_THREADS("workerThreads", ServerConfig::getWorkerThreads),
    BUFFER_SIZE("bufferSize", ServerConfig::getBufferSize),
    BACKLOG("backlog", ServerConfig::getBacklog),
    SERVER_STRING("serverString", ServerConfig::getServerString),
    ALWAYS_SET_DATE("alwaysSetDate", ServerConfig::isAlwaysSetDate),
    MAX_TRANSFER_FILE_SIZE("maxTransferFileSize", ServerConfig::getMaxTransferFileSize),
    ALLOW_UNESCAPED_CHARACTERS_IN_URL("allowUnescapedCharactersInUrl", ServerConfig::isAllowUnescapedCharactersInUrl),
    SHUTDOWN_TIMEOUT("shutdownTimeout", ServerConfig::getShutdownTimeout);

    private final String value;

    private final Function<ServerConfig, Object> effectiveValue;

    ServerOption(String serverOption, Function<ServerConfig, Object> effectiveValue) {
        this.value = serverOption;
        this.effectiveValue = effectiveValue;
    }

    public String value() {
        return this.value;
    }

    /** Validate fields and return corrections for explicitly configured options only. */
    static Map<String, Correction> validateServerOptions(Map<String, Object> mapConfig, ServerConfig serverConfig,
                                                    boolean warnInvalid) {
        Map<String, Correction> corrections = new HashMap<>();
        for (ServerOption serverOption : ServerOption.values()) {
            if (mapConfig != null && mapConfig.containsKey(serverOption.value())) {
                Object value = mapConfig.get(serverOption.value());
                boolean valid = setServerOption(serverOption, value, serverConfig);
                if (!valid && value != null && warnInvalid) {
                    ServerConfig.logger.warn("Server option: " + serverOption.value() + " set in server.yml is invalid, has been reset to default value.");
                }
                if (!valid || value == null) {
                    corrections.put(serverOption.value(), new Correction(value, serverOption.effectiveValue.apply(serverConfig)));
                }
            } else {
                setToDefaultServerOption(serverOption, serverConfig);
            }
        }
        return corrections;
    }

    /** Capture the validated input so a later mutable-map update cannot change the comparison. */
    record Correction(Object originalValue, Object effectiveValue) {}

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
