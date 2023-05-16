package com.networknt.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

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
    private static final String ENABLE_VERIFY_SWT = "enableVerifySwt";
    private static final String ENABLE_EXTRACT_SCOPE_TOKEN = "enableExtractScopeToken";
    private static final String ENABLE_VERIFY_SCOPE = "enableVerifyScope";
    private static final String SKIP_VERIFY_SCOPE_WITHOUT_SPEC = "skipVerifyScopeWithoutSpec";
    private static final String ENABLE_MOCK_JWT = "enableMockJwt";
    private static final String JWT = "jwt";
    private static final String CERTIFICATE = "certificate";
    private static final String CLOCK_SKEW_IN_SECONDS = "clockSkewInSeconds";
    private static final String KEY_RESOLVER = "keyResolver";
    private static final String LOG_JWT_TOKEN = "logJwtToken";
    private static final String LOG_CLIENT_USER_SCOPE = "logClientUserScope";
    private static final String ENABLE_JWT_CACHE = "enableJwtCache";
    private static final String JWT_CACHE_FULL_SIZE = "jwtCacheFullSize";
    private static final String BOOTSTRAP_FROM_KEY_SERVICE = "bootstrapFromKeyService";
    private static final String IGNORE_JWT_EXPIRY = "ignoreJwtExpiry";
    private static final String PROVIDER_ID = "providerId";
    private static final String ENABLE_H2C = "enableH2c";

    private static final String ENABLE_RELAXED_KEY_CONSTRAINTS = "enableRelaxedKeyValidation";
    private static final String SKIP_PATH_PREFIXES = "skipPathPrefixes";
    private static final String PASS_THROUGH_CLAIMS = "passThroughClaims";

    private Map<String, Object> mappedConfig;
    private Map<String, Object> certificate;
    private Config config;
    private boolean enableVerifyJwt;
    private boolean enableVerifySwt;
    private boolean enableExtractScopeToken;
    private boolean enableVerifyScope;
    private boolean skipVerifyScopeWithoutSpec;
    private boolean enableMockJwt;
    private int clockSkewInSeconds;
    private String keyResolver;
    private boolean logJwtToken;
    private boolean logClientUserScope;
    private boolean enableJwtCache;
    private int jwtCacheFullSize;
    private boolean bootstrapFromKeyService;
    private boolean ignoreJwtExpiry;
    private String providerId;
    private boolean enableH2c;

    private boolean enableRelaxedKeyValidation;
    private List<String> skipPathPrefixes;

    private Map<String, String> passThroughClaims;


    private SecurityConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setCertificate();
        setConfigData();
        setSkipPathPrefixes();
        setPassThroughClaims();
    }

    public static SecurityConfig load(String configName) {
        return new SecurityConfig(configName);
    }

    public void reload(String configName) {
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setCertificate();
        setConfigData();
        setSkipPathPrefixes();
        setPassThroughClaims();
    }

    public Map<String, Object> getCertificate() {
        return certificate;
    }

    public boolean isEnableVerifyJwt() {
        return enableVerifyJwt;
    }
    public boolean isEnableVerifySwt() {
        return enableVerifySwt;
    }

    public boolean isEnableH2c() { return enableH2c; }

    public boolean isEnableRelaxedKeyValidation() { return enableRelaxedKeyValidation; }

    public boolean isEnableExtractScopeToken() {
        return enableExtractScopeToken;
    }

    public boolean isEnableVerifyScope() {
        return enableVerifyScope;
    }

    public boolean isSkipVerifyScopeWithoutSpec() {
        return skipVerifyScopeWithoutSpec;
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

    public int getJwtCacheFullSize() {
        return jwtCacheFullSize;
    }
    public boolean isBootstrapFromKeyService() {
        return bootstrapFromKeyService;
    }
    public List<String> getSkipPathPrefixes() {
        return skipPathPrefixes;
    }
    public Map<String, String> getPassThroughClaims() { return passThroughClaims; }
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }
    public String getProviderId() {
        return providerId;
    }
    Config getConfig() {
        return config;
    }

    private void setCertificate() {
        if(getMappedConfig() != null) {
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
                certificate = new HashMap<>();
            }
        }
    }

    private void setConfigData() {
        if(getMappedConfig() != null) {
            Object object = getMappedConfig().get(ENABLE_VERIFY_JWT);
            if(object != null && (Boolean) object) {
                enableVerifyJwt = true;
            }
            object = getMappedConfig().get(ENABLE_VERIFY_SWT);
            if(object != null && (Boolean) object) {
                enableVerifySwt = true;
            }
            object = getMappedConfig().get(ENABLE_H2C);
            if(object != null && (Boolean) object) {
                enableH2c = true;
            }
            object = getMappedConfig().get(ENABLE_RELAXED_KEY_CONSTRAINTS);
            if (object != null && (Boolean) object) {
               enableRelaxedKeyValidation = true;
            }
            object = getMappedConfig().get(ENABLE_EXTRACT_SCOPE_TOKEN);
            if(object != null && (Boolean) object) {
                enableExtractScopeToken = true;
            }
            object = getMappedConfig().get(ENABLE_VERIFY_SCOPE);
            if(object != null && (Boolean) object) {
                enableVerifyScope = true;
            }
            object = getMappedConfig().get(SKIP_VERIFY_SCOPE_WITHOUT_SPEC);
            if(object != null && (Boolean) object) {
                skipVerifyScopeWithoutSpec = true;
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
            object = getMappedConfig().get(JWT_CACHE_FULL_SIZE);
            if(object != null ) {
                jwtCacheFullSize = (Integer)object;
            }
            object = getMappedConfig().get(BOOTSTRAP_FROM_KEY_SERVICE);
            if(object != null && (Boolean) object) {
                bootstrapFromKeyService = true;
            }
            object = getMappedConfig().get(IGNORE_JWT_EXPIRY);
            if(object != null && (Boolean) object) {
                ignoreJwtExpiry = true;
            }
            object = getMappedConfig().get(PROVIDER_ID);
            if(object != null) providerId = (String)object;

            Map<String, Object> jwtMap = (Map)getMappedConfig().get(JWT);
            if(jwtMap != null) {
                clockSkewInSeconds = (Integer) jwtMap.get(CLOCK_SKEW_IN_SECONDS);
                keyResolver = (String) jwtMap.get(KEY_RESOLVER);
            }
        }
    }

    private void setSkipPathPrefixes() {
        if (mappedConfig != null && mappedConfig.get(SKIP_PATH_PREFIXES) != null) {
            Object object = mappedConfig.get(SKIP_PATH_PREFIXES);
            skipPathPrefixes = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        skipPathPrefixes = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the skipPathPrefixes json with a list of strings.");
                    }
                } else {
                    // comma separated
                    skipPathPrefixes = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List)object;
                prefixes.forEach(item -> {
                    skipPathPrefixes.add((String)item);
                });
            } else {
                throw new ConfigException("skipPathPrefixes must be a string or a list of strings.");
            }
        }
    }

    private void setPassThroughClaims() {
        if(mappedConfig != null && mappedConfig.get(PASS_THROUGH_CLAIMS) != null) {
            Object obj = mappedConfig.get(PASS_THROUGH_CLAIMS);
            if(obj instanceof String) {
                String s = (String)obj;
                if(logger.isTraceEnabled()) logger.trace("s = " + s);
                if(s.startsWith("{")) {
                    // json map
                    try {
                        passThroughClaims = Config.getInstance().getMapper().readValue(s, Map.class);
                    } catch (IOException e) {
                        logger.error("IOException:", e);
                    }
                } else {
                    passThroughClaims = new HashMap<>();
                    for(String keyValue : s.split(" *& *")) {
                        String[] pairs = keyValue.split(" *= *", 2);
                        passThroughClaims.put(pairs[0], pairs[1]);
                    }
                }
            } else if (obj instanceof Map) {
                passThroughClaims = (Map)obj;
            } else {
                logger.error("passThroughClaims is the wrong type. Only JSON map or YAML map is supported.");
            }
        }
    }

}
