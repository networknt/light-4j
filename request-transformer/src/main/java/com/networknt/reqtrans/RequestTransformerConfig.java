package com.networknt.reqtrans;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";

    private Map<String, Object> mappedConfig;
    private Config config;
    private boolean enabled;
    private boolean requiredContent;
    List<String> appliedPathPrefixes;

    private RequestTransformerConfig() {
        this(CONFIG_NAME);
    }

    private RequestTransformerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
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
        setConfigList();
    }


    public boolean isEnabled() {
        return enabled;
    }

    public boolean isRequiredContent() { return requiredContent; }
    public List<String> getAppliedPathPrefixes() {
        return appliedPathPrefixes;
    }

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

    private void setConfigList() {
        if (mappedConfig.get(APPLIED_PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(APPLIED_PATH_PREFIXES);
            appliedPathPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        appliedPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the appliedPathPrefixes json with a list of strings.");
                    }
                } else {
                    // comma separated
                    appliedPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    appliedPathPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("appliedPathPrefixes must be a string or a list of strings.");
            }
        }
    }

}
