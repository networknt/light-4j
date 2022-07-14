package com.networknt.handler;

import com.networknt.config.Config;

import java.util.Map;

/**
 * The config class for the ResponseInterceptorInjectionHandler middleware handler.
 *
 */
public class ResponseInjectionConfig {
    public static final String CONFIG_NAME = "response-injection";
    private static final String ENABLED = "enabled";

    private boolean enabled;

    private Map<String, Object> mappedConfig;
    private Config config;

    public ResponseInjectionConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    public ResponseInjectionConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    static ResponseInjectionConfig load() {
        return new ResponseInjectionConfig();
    }

    static ResponseInjectionConfig load(String configName) {
        return new ResponseInjectionConfig(configName);
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
