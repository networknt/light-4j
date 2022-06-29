package com.networknt.restrans;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class ResponseTransformerConfig {
    public static final String CONFIG_NAME = "response-transformer";
    private static final Logger logger = LoggerFactory.getLogger(ResponseTransformerConfig.class);

    private static final String ENABLED = "enabled";

    private Map<String, Object> mappedConfig;
    private Config config;
    private boolean enabled;

    private ResponseTransformerConfig() {
        this(CONFIG_NAME);
    }

    private ResponseTransformerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static ResponseTransformerConfig load() {
        return new ResponseTransformerConfig();
    }

    public static ResponseTransformerConfig load(String configName) {
        return new ResponseTransformerConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }


    public boolean isEnabled() {
        return enabled;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null && (Boolean) object) {
            enabled = true;
        }
    }

}
