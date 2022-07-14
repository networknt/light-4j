package com.networknt.reqtrans;

import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * This is a generic middleware handler to manipulate request based on rule-engine rules so that it can be much more
 * flexible than any other handlers like the header handler to manipulate the headers. The rules will be loaded from
 * the configuration or from the light-portal if portal is implemented.
 *
 * @author Steve Hu
 */
public class RequestTransformerConfig {
    public static final String CONFIG_NAME = "request-transformer";
    private static final Logger logger = LoggerFactory.getLogger(RequestTransformerConfig.class);

    private static final String ENABLED = "enabled";
    private static final String REQUIRED_CONTENT = "requiredContent";
    private Map<String, Object> mappedConfig;
    private Config config;
    private boolean enabled;
    private boolean requiredContent;

    private RequestTransformerConfig() {
        this(CONFIG_NAME);
    }

    private RequestTransformerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static RequestTransformerConfig load() {
        return new RequestTransformerConfig();
    }

    public static RequestTransformerConfig load(String configName) {
        return new RequestTransformerConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }


    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequiredContent() { return requiredContent; }
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null && (Boolean) object) {
            enabled = true;
        }
        object = mappedConfig.get(REQUIRED_CONTENT);
        if(object != null && (Boolean) object) {
            requiredContent = true;
        }
    }

}
