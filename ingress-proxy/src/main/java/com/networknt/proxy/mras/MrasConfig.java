package com.networknt.proxy.mras;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MrasConfig {
    public static final String CONFIG_NAME = "mras";
    private static final String ENABLED = "enabled";
    private static final String TOKEN_URL = "tokenUrl";

    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String CACHE_ENABLED = "cacheEnabled";
    private static final String MEM_KEY = "memKey";
    private static final String GRACE_PERIOD = "gracePeriod";
    private static final String KEY_STORE_ALIAS = "keyStoreAlias";
    private static final String KEY_ALIAS = "keyAlias";
    private static final String CERT_FILENAME = "certFilename";
    private static final String CERT_PASSWORD = "certPassword";
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
    String keyStoreAlias;
    String keyAlias;
    String certFilename;
    String certPassword;
    String proxyHost;
    int proxyPort;
    boolean enableHttp2;
    List<String> appliedPathPrefixes;
    String serviceHost;
    private Config config;
    private Map<String, Object> mappedConfig;

    public MrasConfig() {
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
    public MrasConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
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
    public String getTokenUrl() {
        return tokenUrl;
    }
    public void setTokenUrl(String tokenUrl) {
        this.tokenUrl = tokenUrl;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isCacheEnabled() {
        return cacheEnabled;
    }

    public void setCacheEnabled(boolean cacheEnabled) {
        this.cacheEnabled = cacheEnabled;
    }

    public String getMemKey() {
        return memKey;
    }

    public void setMemKey(String memKey) {
        this.memKey = memKey;
    }

    public int getGracePeriod() {
        return gracePeriod;
    }

    public void setGracePeriod(int gracePeriod) {
        this.gracePeriod = gracePeriod;
    }

    public String getKeyStoreAlias() {
        return keyStoreAlias;
    }

    public void setKeyStoreAlias(String keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
    }

    public String getKeyAlias() {
        return keyAlias;
    }

    public void setKeyAlias(String keyAlias) {
        this.keyAlias = keyAlias;
    }

    public String getCertFilename() {
        return certFilename;
    }

    public void setCertFilename(String certFilename) {
        this.certFilename = certFilename;
    }

    public String getCertPassword() {
        return certPassword;
    }

    public void setCertPassword(String certPassword) {
        this.certPassword = certPassword;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public String getServiceHost() {
        return serviceHost;
    }

    public void setServiceHost(String serviceHost) {
        this.serviceHost = serviceHost;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null && (Boolean) object) {
            setEnabled((Boolean)object);
        }
        object = mappedConfig.get(TOKEN_URL);
        if (object != null) {
            setTokenUrl((String) object);
        }
        object = mappedConfig.get(USERNAME);
        if (object != null) {
            setUsername((String)object);
        }
        object = mappedConfig.get(PASSWORD);
        if (object != null) {
            setPassword((String)object);
        }
        object = mappedConfig.get(CACHE_ENABLED);
        if (object != null && (Boolean) object) {
            setCacheEnabled((Boolean)object);
        }
        object = mappedConfig.get(MEM_KEY);
        if (object != null) {
            setMemKey((String)object);
        }
        object = mappedConfig.get(GRACE_PERIOD);
        if (object != null) {
            setGracePeriod((int) object);
        }
        object = mappedConfig.get(KEY_STORE_ALIAS);
        if (object != null) {
            setKeyStoreAlias((String)object);
        }
        object = mappedConfig.get(KEY_ALIAS);
        if (object != null) {
            setKeyAlias((String)object);
        }
        object = mappedConfig.get(CERT_FILENAME);
        if (object != null) {
            setCertFilename((String)object);
        }
        object = mappedConfig.get(CERT_PASSWORD);
        if (object != null) {
            setCertPassword((String)object);
        }
        object = mappedConfig.get(PROXY_HOST);
        if (object != null) {
            setProxyHost((String) object);
        }
        object = mappedConfig.get(PROXY_PORT);
        if (object != null) {
            setProxyPort((int) object);
        }
        object = mappedConfig.get(ENABLE_HTTP2);
        if (object != null && (Boolean) object) {
            setEnableHttp2((Boolean)object);
        }
        object = mappedConfig.get(SERVICE_HOST);
        if (object != null) {
            setServiceHost((String) object);
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
                // there is only one path available
                appliedPathPrefixes.add((String)object);
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
