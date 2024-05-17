package com.networknt.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class OAuthServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(OAuthServerConfig.class);
    public static final String CONFIG_NAME = "oauthServer";

    private static final String ENABLED = "enabled";
    private static final String GET_METHOD_ENABLED = "getMethodEnabled";
    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final String PASS_THROUGH = "passThrough";
    private static final String TOKEN_SERVICE_ID = "tokenServiceId";
    private Map<String, Object> mappedConfig;
    private final Config config;
    private boolean enabled;
    private boolean getMethodEnabled;
    private boolean passThrough;
    private String tokenServiceId;
    List<String> clientCredentials;

    private OAuthServerConfig() {
        this(CONFIG_NAME);
    }

    private OAuthServerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }

    public static OAuthServerConfig load() {
        return new OAuthServerConfig();
    }

    public static OAuthServerConfig load(String configName) {
        return new OAuthServerConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }


    public boolean isEnabled() {
        return enabled;
    }

    public boolean isGetMethodEnabled() {
        return getMethodEnabled;
    }
    public List<String> getClientCredentials() {
        return clientCredentials;
    }

    public boolean isPassThrough() {
        return passThrough;
    }

    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
    }

    public String getTokenServiceId() {
        return tokenServiceId;
    }

    public void setTokenServiceId(String tokenServiceId) {
        this.tokenServiceId = tokenServiceId;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = mappedConfig.get(GET_METHOD_ENABLED);
        if(object != null) getMethodEnabled = Config.loadBooleanValue(GET_METHOD_ENABLED, object);
        object = mappedConfig.get(PASS_THROUGH);
        if(object != null) passThrough = Config.loadBooleanValue(PASS_THROUGH, object);
        tokenServiceId = (String)getMappedConfig().get(TOKEN_SERVICE_ID);
    }

    private void setConfigList() {
        if (mappedConfig.get(CLIENT_CREDENTIALS) != null) {
            Object object = mappedConfig.get(CLIENT_CREDENTIALS);
            clientCredentials = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        clientCredentials = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the clientCredentials json with a list of strings.");
                    }
                } else {
                    // comma separated
                    clientCredentials = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    clientCredentials.add((String)item);
                });
            } else {
                throw new ConfigException("clientCredentials must be a string or a list of strings.");
            }
        }
    }
}
