package com.networknt.token.limit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class TokenLimitConfig {
    private static final Logger logger = LoggerFactory.getLogger(TokenLimitConfig.class);
    public static final String CONFIG_NAME = "token-limit";
    ;
    private static final String ENABLED = "enabled";
    private static final String ERROR_ON_LIMIT = "errorOnLimit";
    private static final String DUPLICATE_LIMIT = "duplicateLimit";
    private static final String TOKEN_PATH_TEMPLATES = "tokenPathTemplates";
    private static final String LEGACY_CLIENT = "legacyClient";
    private static final String EXPIRE_KEY = "expireKey";

    boolean enabled;
    boolean errorOnLimit;
    int duplicateLimit;
    List<String> tokenPathTemplates;
    List<String> legacyClient;
    String expireKey;

    private Map<String, Object> mappedConfig;
    private final Config config;

    private TokenLimitConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private TokenLimitConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setTokenPathTemplatesList();
        setLegacyClientList();
    }

    public static TokenLimitConfig load() {
        return new TokenLimitConfig();
    }

    public static TokenLimitConfig load(String configName) {
        return new TokenLimitConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isErrorOnLimit() {
        return errorOnLimit;
    }

    public void setErrorOnLimit(boolean errorOnLimit) {
        this.errorOnLimit = errorOnLimit;
    }

    public int getDuplicateLimit() {
        return duplicateLimit;
    }

    public void setDuplicateLimit(int duplicateLimit) {
        this.duplicateLimit = duplicateLimit;
    }

    public List<String> getTokenPathTemplates() {
        return tokenPathTemplates;
    }

    public void setTokenPathTemplates(List<String> tokenPathTemplates) {
        this.tokenPathTemplates = tokenPathTemplates;
    }

    public List<String> getLegacyClient() {
        return legacyClient;
    }

    public void setLegacyClient(List<String> legacyClient) {
        this.legacyClient = legacyClient;
    }

    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public String getExpireKey() {
        return expireKey;
    }
    public void setExpireKey(String expireKey) {
        this.expireKey = expireKey;
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = getMappedConfig().get(ERROR_ON_LIMIT);
        if(object != null) errorOnLimit = Config.loadBooleanValue(ERROR_ON_LIMIT, object);
        object = mappedConfig.get(DUPLICATE_LIMIT);
        if (object != null) duplicateLimit = Config.loadIntegerValue(DUPLICATE_LIMIT, object);
        object = getMappedConfig().get(EXPIRE_KEY);
        if(object != null) expireKey = ((String) object);
    }

    private void setTokenPathTemplatesList() {
        if (mappedConfig != null && mappedConfig.get(TOKEN_PATH_TEMPLATES) != null) {
            Object object = mappedConfig.get(TOKEN_PATH_TEMPLATES);
            tokenPathTemplates = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = {}", s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        tokenPathTemplates = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the tokenPathTemplates json with a list of strings.");
                    }
                } else {
                    // comma separated
                    tokenPathTemplates = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                tokenPathTemplates = (List<String>) getMappedConfig().get(TOKEN_PATH_TEMPLATES);
            } else {
                throw new ConfigException("tokenPathTemplates must be a string or a list of strings.");
            }
        }

    }

    private void setLegacyClientList() {
        if (mappedConfig != null && mappedConfig.get(LEGACY_CLIENT) != null) {
            Object object = mappedConfig.get(LEGACY_CLIENT);
            legacyClient = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = {}", s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        legacyClient = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the Legacy Client json with a list of strings.");
                    }
                } else {
                    // comma separated
                    legacyClient = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                legacyClient = (List<String>) getMappedConfig().get(LEGACY_CLIENT);
            } else {
                throw new ConfigException("legacyClient must be a string or a list of strings.");
            }
        }

    }
}
