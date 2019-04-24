package com.networknt.client;

import com.networknt.common.DecryptUtil;
import com.networknt.config.Config;

import java.util.Map;


public final class ClientConfig {

    public static final String CONFIG_NAME = "client";
    public static final String CONFIG_SECRET = "secret";
    public static final String REQUEST = "request";
    public static int DEFAULT_BUFFER_SIZE = 24; // 24*1024 buffer size will be good for most of the app.
    public static final int DEFAULT_ERROR_THRESHOLD = 5;
    public static final int DEFAULT_TIMEOUT = 3000;
    public static final int DEFAULT_RESET_TIMEOUT = 600000;
    private static final String BUFFER_SIZE = "bufferSize";
    private static final String ERROR_THRESHOLD = "errorThreshold";
    private static final String TIMEOUT = "timeout";
    private static final String RESET_TIMEOUT = "resetTimeout";
    private static final String OAUTH = "oauth";
    private static final String TOKEN = "token";

    private final Config config;
    private final Map<String, Object> mappedConfig;

    private Map<String, Object> tokenConfig;
    private Map<String, Object> secretConfig;
    private int bufferSize = DEFAULT_BUFFER_SIZE;
    private int resetTimeout = DEFAULT_RESET_TIMEOUT;
    private int timeout = DEFAULT_TIMEOUT;
    private int errorThreshold = DEFAULT_ERROR_THRESHOLD;

    private static ClientConfig instance;

    private ClientConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(CONFIG_NAME);

        if (mappedConfig != null) {
            setBufferSize();
            setTokenConfig();
            setSecretConfig();
            setRequestConfig();
        }
    }

    public static ClientConfig get() {
        if (instance == null) {
            instance = new ClientConfig();
        }
        return instance;
    }

    /**
     * For testing purpose
     */
    static void reset() {
        instance = null;
    }

    private void setSecretConfig() {
        secretConfig = Config.getInstance().getJsonMapConfig(CONFIG_SECRET);
    }

    private void setRequestConfig() {
        if (!mappedConfig.containsKey(REQUEST)) {
            return;
        }
        Map<String, Object> requestConfig = (Map<String, Object>) mappedConfig.get(REQUEST);

        if (requestConfig.containsKey(RESET_TIMEOUT)) {
            resetTimeout = (int) requestConfig.get(RESET_TIMEOUT);
        }
        if (requestConfig.containsKey(ERROR_THRESHOLD)) {
            errorThreshold = (int) requestConfig.get(ERROR_THRESHOLD);
        }
        if (requestConfig.containsKey(TIMEOUT)) {
            timeout = (int) requestConfig.get(TIMEOUT);
        }
    }

    private void setBufferSize() {
        Object bufferSizeObject = mappedConfig.get(BUFFER_SIZE);
        if (bufferSizeObject != null) {
            bufferSize = (int) bufferSizeObject;
        }
    }

    private void setTokenConfig() {
        Map<String, Object> oauthConfig = (Map<String, Object>)mappedConfig.get(OAUTH);
        if (oauthConfig != null) {
            tokenConfig = (Map<String, Object>)oauthConfig.get(TOKEN);
        }
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public Config getConfig() {
        return config;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public Map<String, Object> getTokenConfig() {
        return tokenConfig;
    }

    public Map<String, Object> getSecretConfig() {
        return secretConfig;
    }

    public int getTimeout() {
        return timeout;
    }

    public int getResetTimeout() {
        return resetTimeout;
    }

    public int getErrorThreshold() {
        return errorThreshold;
    }
}
