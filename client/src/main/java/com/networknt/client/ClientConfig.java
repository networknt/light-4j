package com.networknt.client;

import com.networknt.common.DecryptUtil;
import com.networknt.config.Config;

import java.util.Map;


public class ClientConfig {

    public static final String CONFIG_NAME = "client";
    public static final String CONFIG_SECRET = "secret";
    public static int DEFAULT_BUFFER_SIZE = 24; // 24*1024 buffer size will be good for most of the app.
    static final int DEFAULT_ERROR_THRESHOLD = 5;
    static final int DEFAULT_TIMEOUT = 10000;
    static final int DEFAULT_RESET_TIMEOUT = 600000;
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
            setErrorThreshold();
            setTimeout();
            setResetTimeout();
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
        Map<String, Object> secretMap = Config.getInstance().getJsonMapConfig(CONFIG_SECRET);
        if(secretMap != null) {
            secretConfig = DecryptUtil.decryptMap(secretMap);
        } else {
            throw new ExceptionInInitializerError("Could not locate secret.yml");
        }
    }

    private void setResetTimeout() {
        if (mappedConfig.containsKey(RESET_TIMEOUT)) {
            resetTimeout = (int) mappedConfig.get(RESET_TIMEOUT);
        }
    }

    private void setTimeout() {
        if (mappedConfig.containsKey(TIMEOUT)) {
            timeout = (int) mappedConfig.get(TIMEOUT);
        }
    }

    private void setErrorThreshold() {
        if (mappedConfig.containsKey(ERROR_THRESHOLD)) {
            errorThreshold = (int) mappedConfig.get(ERROR_THRESHOLD);
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
