package com.networknt.apikey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiKeyConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyConfig.class);

    public static final String CONFIG_NAME = "apikey";
    public static final String ENABLED = "enabled";
    public static final String PATH_PREFIX = "pathPrefix";
    public static final String HEADER_NAME = "headerName";
    public static final String API_KEY = "apiKey";
    public static final String PATH_PREFIX_AUTHS = "pathPrefixAuths";

    boolean enabled;
    List<ApiKey> pathPrefixAuths;
    private Config config;
    private Map<String, Object> mappedConfig;

    private ApiKeyConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private ApiKeyConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }
    public static ApiKeyConfig load() {
        return new ApiKeyConfig();
    }

    public static ApiKeyConfig load(String configName) {
        return new ApiKeyConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<ApiKey> getPathPrefixAuths() {
        return pathPrefixAuths;
    }

    public void setPathPrefixAuths(List<ApiKey> pathPrefixAuths) {
        this.pathPrefixAuths = pathPrefixAuths;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null && (Boolean) object) {
            setEnabled(true);
        }
    }

    private void setConfigList() {
        // path prefix auth mapping
        if (mappedConfig.get(PATH_PREFIX_AUTHS) != null) {
            Object object = mappedConfig.get(PATH_PREFIX_AUTHS);
            pathPrefixAuths = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("pathPrefixAuth s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        pathPrefixAuths = Config.getInstance().getMapper().readValue(s, new TypeReference<List<ApiKey>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the pathPrefixAuth json with a list of string and object.");
                    }
                } else {
                    throw new ConfigException("pathPrefixAuth must be a list of string object map.");
                }
            } else if (object instanceof List) {
                // the object is a list of map, we need convert it to PathPrefixAuth object.
                List<Map<String, Object>> values = (List<Map<String, Object>>)object;
                for(Map<String, Object> value: values) {
                    ApiKey apiKey = new ApiKey();
                    apiKey.setPathPrefix((String)value.get(PATH_PREFIX));
                    apiKey.setHeaderName((String)value.get(HEADER_NAME));
                    apiKey.setApiKey((String)value.get(API_KEY));
                    pathPrefixAuths.add(apiKey);
                }
            } else {
                throw new ConfigException("pathPrefixAuth must be a list of string object map.");
            }
        }
    }
}
