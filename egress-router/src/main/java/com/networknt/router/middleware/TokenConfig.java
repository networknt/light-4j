package com.networknt.router.middleware;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.handler.config.MethodRewriteRule;
import com.networknt.handler.config.QueryHeaderRewriteRule;
import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.router.RouterConfig;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TokenConfig {
    private static final Logger logger = LoggerFactory.getLogger(TokenConfig.class);
    static final String CONFIG_NAME = "token";
    private static final String ENABLED = "enabled";
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";

    boolean enabled;
    List<String> appliedPathPrefixes;

    private Config config;
    private Map<String, Object> mappedConfig;

    private TokenConfig() {
        this(CONFIG_NAME);
    }

    private TokenConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigList();
        setConfigData();
    }

    public static TokenConfig load() {
        return new TokenConfig();
    }

    public static TokenConfig load(String configName) {
        return new TokenConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigList();
        setConfigData();
    }
    public void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null && (Boolean) object) {
            enabled = true;
        }
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getAppliedPathPrefixes() {
        return appliedPathPrefixes;
    }

    public void setAppliedPathPrefixes(List<String> appliedPathPrefixes) {
        this.appliedPathPrefixes = appliedPathPrefixes;
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
