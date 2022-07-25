package com.networknt.proxy.mras;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class MrasConfig {
    private static final Logger logger = LoggerFactory.getLogger(MrasConfig.class);
    public static final String CONFIG_NAME = "mras";
    private static final String ENABLED = "enabled";
    private static final String TOKEN_URL = "tokenUrl";

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String CACHE_ENABLED = "cacheEnabled";
    private static final String MEM_KEY = "memKey";
    private static final String GRACE_PERIOD = "gracePeriod";
    private static final String KEY_STORE_NAME = "keyStoreName";
    private static final String KEY_STORE_PASS = "keyStorePass";
    private static final String KEY_PASS = "keyPass";

    private static final String TRUST_STORE_NAME = "trustStoreName";
    private static final String TRUST_STORE_PASS = "trustStorePass";
    private static final String PROXY_HOST = "proxyHost";
    private static final String PROXY_PORT = "proxyPort";
    private static final String ENABLE_HTTP2 = "enableHttp2";
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";
    private static final String SERVICE_HOST = "serviceHost";

    boolean enabled;
    String tokenUrl;
    String username;
    String password;
    boolean cacheEnabled;
    String memKey;
    int gracePeriod;
    String keyStoreName;
    String keyStorePass;
    String keyPass;
    String trustStoreName;
    String trustStorePass;
    String proxyHost;
    int proxyPort;
    boolean enableHttp2;
    List<String> appliedPathPrefixes;
    String serviceHost;
    private Config config;
    private Map<String, Object> mappedConfig;

    private MrasConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private MrasConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }

    public static MrasConfig load() {
        return new MrasConfig();
    }

    public static MrasConfig load(String configName) {
        return new MrasConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public String getMemKey() {
        return memKey;
    }

    public int getGracePeriod() {
        return gracePeriod;
    }

    public String getKeyStoreName() {
        return keyStoreName;
    }

    public String getKeyStorePass() {
        return keyStorePass;
    }

    public String getKeyPass() {
        return keyPass;
    }

    public String getTrustStoreName() {
        return trustStoreName;
    }

    public String getTrustStorePass() {
        return trustStorePass;
    }


    public String getProxyHost() {
        return proxyHost;
    }
    public int getProxyPort() {
        return proxyPort;
    }


    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null && (Boolean) object) {
            enabled = (Boolean)object;
        }
        object = mappedConfig.get(TOKEN_URL);
        if (object != null) {
            tokenUrl = (String) object;
        }
        object = mappedConfig.get(USERNAME);
        if (object != null) {
            username = (String)object;
        }
        object = mappedConfig.get(PASSWORD);
        if (object != null) {
            password = (String)object;
        }
        object = mappedConfig.get(CACHE_ENABLED);
        if (object != null && (Boolean) object) {
            cacheEnabled = (Boolean)object;
        }
        object = mappedConfig.get(MEM_KEY);
        if (object != null) {
            memKey = (String)object;
        }
        object = mappedConfig.get(GRACE_PERIOD);
        if (object != null) {
            gracePeriod = (Integer)object;
        }
        object = mappedConfig.get(KEY_STORE_NAME);
        if (object != null) {
            keyStoreName = (String)object;
        }
        object = mappedConfig.get(KEY_STORE_PASS);
        if (object != null) {
            keyStorePass = (String)object;
        }
        object = mappedConfig.get(KEY_PASS);
        if (object != null) {
            keyPass = (String)object;
        }
        object = mappedConfig.get(TRUST_STORE_NAME);
        if (object != null) {
            trustStoreName = (String)object;
        }
        object = mappedConfig.get(TRUST_STORE_PASS);
        if (object != null) {
            trustStorePass = (String)object;
        }
        object = mappedConfig.get(PROXY_HOST);
        if (object != null) {
            proxyHost = (String) object;
        }
        object = mappedConfig.get(PROXY_PORT);
        if (object != null) {
            proxyPort = (Integer) object;
        }
        object = mappedConfig.get(ENABLE_HTTP2);
        if (object != null && (Boolean) object) {
            enableHttp2 = (Boolean)object;
        }
        object = mappedConfig.get(SERVICE_HOST);
        if (object != null) {
            serviceHost = (String) object;
        }
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
