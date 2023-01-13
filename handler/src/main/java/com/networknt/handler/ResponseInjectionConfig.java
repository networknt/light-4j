package com.networknt.handler;

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
 * The config class for the ResponseInterceptorInjectionHandler middleware handler.
 *
 */
public class ResponseInjectionConfig {
    private static final Logger logger = LoggerFactory.getLogger(ResponseInjectionConfig.class);

    public static final String CONFIG_NAME = "response-injection";
    private static final String ENABLED = "enabled";
    private static final String APPLIED_BODY_INJECTION_PATH_PREFIXES = "appliedBodyInjectionPathPrefixes";
    private boolean enabled;
    private List<String> appliedBodyInjectionPathPrefixes;

    private Map<String, Object> mappedConfig;
    private Config config;

    public ResponseInjectionConfig() {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
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
        setConfigList();
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
        setConfigList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<String> getAppliedBodyInjectionPathPrefixes() {
        return appliedBodyInjectionPathPrefixes;
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

    private void setConfigList() {
        if (mappedConfig != null && mappedConfig.get(APPLIED_BODY_INJECTION_PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(APPLIED_BODY_INJECTION_PATH_PREFIXES);
            appliedBodyInjectionPathPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        appliedBodyInjectionPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the appliedBodyInjectionPathPrefixes json with a list of strings.");
                    }
                } else {
                    // comma separated
                    appliedBodyInjectionPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    appliedBodyInjectionPathPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("appliedBodyInjectionPathPrefixes must be a string or a list of strings.");
            }
        }
    }

}
