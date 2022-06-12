package com.networknt.security;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Create this class to load security.yml or openapi-security or graphql-security
 * or hybrid-security configuration file with map as default value.
 *
 * Note: there is no static CONFIG_NAME in this class and user has to pass in the
 * name according to the framework that is used.
 *
 * @author Steve Hu
 */
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private static final String ENABLE_VERIFY_JWT = "enableVerifyJwt";
    private static final String ENABLE_EXTRACT_SCOPE_TOKEN = "enableExtractScopeToken";
    private static final String ENABLE_VERIFY_SCOPE = "enableVerifyScope";
    private static final String ENABLE_VERIFY_JWT_SCOPE_TOKEN = "enableVerifyJWTScopeToken";
    private static final String ENABLE_MOCK_JWT = "enableMockJwt";
    private static final String JWT = "jwt";
    private static final String CERTIFICATE = "certificate";
    private static final String CLOCK_SKEW_IN_SECONDS = "clockSkewInSeconds";
    private static final String KEY_RESOLVER = "keyResolver";
    private static final String LOG_JWT_TOKEN = "logJwtToken";
    private static final String LOG_CLIENT_USER_SCOPE = "logClientUserScope";
    private static final String ENABLE_JWT_CACHE = "enableJwtCache";
    private static final String BOOTSTRAP_FROM_KEY_SERVICE = "bootstrapFromKeyService";
    private static final String IGNORE_JWT_EXPIRY = "ignoreJwtExpiry";

    private Map<String, Object> mappedConfig;
    private Map<String, Object> certificate;
    private Config config;
    private boolean enableVerifyJwt;
    private boolean enableExtractScopeToken;
    private boolean enableVerifyScope;
    private boolean enableVerifyJwtScopeToken;
    private boolean enableMockJwt;
    private int clockSkewInSeconds;
    private String keyResolver;
    private boolean logJwtToken;
    private boolean logClientUserScope;
    private boolean enableJwtCache;
    private boolean bootstrapFromKeyService;
    private boolean ignoreJwtExpiry;


    private SecurityConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setCertificate();
        setConfigData();
    }

    public static SecurityConfig load(String configName) {
        return new SecurityConfig(configName);
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setCertificate();
        setConfigData();
    }

    public Map<String, Object> getCertificate() {
        return certificate;
    }

    public boolean isEnableVerifyJwt() {
        return enableVerifyJwt;
    }

    public boolean isEnableExtractScopeToken() {
        return enableExtractScopeToken;
    }

    public boolean isEnableVerifyScope() {
        return enableVerifyScope;
    }

    public boolean isEnableVerifyJwtScopeToken() {
        return enableVerifyJwtScopeToken;
    }

    public boolean isIgnoreJwtExpiry() {
        return ignoreJwtExpiry;
    }

    public boolean isEnableMockJwt() {
        return enableMockJwt;
    }

    public int getClockSkewInSeconds() {
        return clockSkewInSeconds;
    }

    public String getKeyResolver() {
        return keyResolver;
    }

    public boolean isLogJwtToken() {
        return logJwtToken;
    }

    public boolean isLogClientUserScope() {
        return logClientUserScope;
    }

    public boolean isEnableJwtCache() {
        return enableJwtCache;
    }

    public boolean isBootstrapFromKeyService() {
        return bootstrapFromKeyService;
    }

    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setCertificate() {
        Map<String, Object> jwtMap = (Map)getMappedConfig().get(JWT);
        Object obj = jwtMap.get(CERTIFICATE);
        if(obj instanceof String) {
            String s = (String)obj;
            if(logger.isTraceEnabled()) logger.trace("s = " + s);
            Map<String, Object> map = new LinkedHashMap<>();
            for(String keyValue : s.split(" *& *")) {
                String[] pairs = keyValue.split(" *= *", 2);
                map.put(pairs[0], pairs.length == 1 ? "" : pairs[1]);
            }
            certificate = map;
        } else if (obj instanceof Map) {
            certificate = (Map)obj;
        } else {
            throw new ConfigException("certificate map is missing or wrong type.");
        }
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLE_VERIFY_JWT);
        if(object != null && (Boolean) object) {
            enableVerifyJwt = true;
        }
        object = getMappedConfig().get(ENABLE_EXTRACT_SCOPE_TOKEN);
        if(object != null && (Boolean) object) {
            enableExtractScopeToken = true;
        }
        object = getMappedConfig().get(ENABLE_VERIFY_SCOPE);
        if(object != null && (Boolean) object) {
            enableVerifyScope = true;
        }
        object = getMappedConfig().get(ENABLE_VERIFY_JWT_SCOPE_TOKEN);
        if(object != null && (Boolean) object) {
            enableVerifyJwtScopeToken = true;
        }
        object = getMappedConfig().get(ENABLE_MOCK_JWT);
        if(object != null && (Boolean) object) {
            enableMockJwt = true;
        }
        object = getMappedConfig().get(LOG_JWT_TOKEN);
        if(object != null && (Boolean) object) {
            logJwtToken = true;
        }
        object = getMappedConfig().get(LOG_CLIENT_USER_SCOPE);
        if(object != null && (Boolean) object) {
            logClientUserScope = true;
        }
        object = getMappedConfig().get(ENABLE_JWT_CACHE);
        if(object != null && (Boolean) object) {
            enableJwtCache = true;
        }
        object = getMappedConfig().get(BOOTSTRAP_FROM_KEY_SERVICE);
        if(object != null && (Boolean) object) {
            bootstrapFromKeyService = true;
        }
        object = getMappedConfig().get(IGNORE_JWT_EXPIRY);
        if(object != null && (Boolean) object) {
            ignoreJwtExpiry = true;
        }

        Map<String, Object> jwtMap = (Map)getMappedConfig().get(JWT);
        clockSkewInSeconds = (Integer)jwtMap.get(CLOCK_SKEW_IN_SECONDS);
        keyResolver = (String)jwtMap.get(KEY_RESOLVER);
    }

}
