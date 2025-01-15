package com.networknt.restrans;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ResponseCacheConfig {
    public static final String CONFIG_NAME = "response-cache";
    private static final Logger logger = LoggerFactory.getLogger(ResponseCacheConfig.class);

    private static final String ENABLED = "enabled";
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";

    private Map<String, Object> mappedConfig;
    private final Config config;
    private boolean enabled;
    List<String> appliedPathPrefixes;

    private ResponseCacheConfig() {
        this(CONFIG_NAME);
    }

    private ResponseCacheConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }

    public static ResponseCacheConfig load() {
        return new ResponseCacheConfig();
    }

    public static ResponseCacheConfig load(String configName) {
        return new ResponseCacheConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }


    public boolean isEnabled() {
        return enabled;
    }
    public List<String> getAppliedPathPrefixes() {
        return appliedPathPrefixes;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        enabled = Config.loadBooleanValue(ENABLED, object);
    }

    private void setConfigList() {
        if (mappedConfig.get(APPLIED_PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(APPLIED_PATH_PREFIXES);
            appliedPathPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = {}", s);
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
