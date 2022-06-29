package com.networknt.handler;

import com.networknt.config.Config;

import java.util.Map;

public class SinkConduitConfig {
    public static final String CONFIG_NAME = "sink-conduit";
    private static final String ENABLED = "enabled";

    private boolean enabled;

    private Map<String, Object> mappedConfig;
    private Config config;

    public SinkConduitConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    public SinkConduitConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    static SinkConduitConfig load() {
        return new SinkConduitConfig();
    }

    static SinkConduitConfig load(String configName) {
        return new SinkConduitConfig(configName);
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
