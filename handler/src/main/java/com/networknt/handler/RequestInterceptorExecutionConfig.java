package com.networknt.handler;

import com.networknt.config.Config;

import java.util.Map;

public class RequestInterceptorExecutionConfig {
    public static final String CONFIG_NAME = "request-interceptor-execution";
    private static final String ENABLED = "enabled";

    private boolean enabled;

    private Map<String, Object> mappedConfig;
    private Config config;

    public RequestInterceptorExecutionConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    public RequestInterceptorExecutionConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    static RequestInterceptorExecutionConfig load() {
        return new RequestInterceptorExecutionConfig();
    }

    static RequestInterceptorExecutionConfig load(String configName) {
        return new RequestInterceptorExecutionConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public boolean isEnabled() {
        return enabled;
    }

    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {

        Object object = getMappedConfig().get(ENABLED);
        if(object != null && (Boolean) object) {
            enabled = true;
        }
    }
}
