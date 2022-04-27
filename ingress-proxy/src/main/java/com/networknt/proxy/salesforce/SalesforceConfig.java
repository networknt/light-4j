package com.networknt.proxy.salesforce;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalesforceConfig {
    public static final String CONFIG_NAME = "salesforce";
    private static final String ENABLED = "enabled";
    private static final String TOKEN_URL = "tokenUrl";
    private static final String AUTH_ISSUER = "authIssuer";
    private static final String AUTH_SUBJECT = "authSubject";
    private static final String AUTH_AUDIENCE = "authAudience";
    private static final String CERT_FILENAME = "certFilename";
    private static final String CERT_PASSWORD = "certPassword";
    private static final String IV = "iv";
    private static final String TOKEN_TTL = "tokenTtl";
    private static final String WAIT_LENGTH = "waitLength";
    private static final String PROXY_HOST = "proxyHost";
    private static final String PROXY_PORT = "proxyPort";
    private static final String ENABLE_HTTP2 = "enableHttps";
    private static final String APPLIED_PATH_PREFIXES = "appliedPathPrefixes";
    private static final String SERVICE_HOST = "serviceHost";

    boolean enabled;
    String tokenUrl;
    String authIssuer;
    String authSubject;
    String authAudience;
    String certFilename;
    String certPassword;
    String iv;
    int tokenTtl;
    int waitLength;
    String proxyHost;
    int proxyPort;
    boolean enableHttp2;
    List<String> appliedPathPrefixes;
    String serviceHost;
    private Config config;
    private Map<String, Object> mappedConfig;

    public SalesforceConfig() {
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
    public SalesforceConfig(String configName) {
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

    public String getAuthIssuer() {
        return authIssuer;
    }

    public void setAuthIssuer(String authIssuer) {
        this.authIssuer = authIssuer;
    }

    public String getAuthSubject() {
        return authSubject;
    }

    public void setAuthSubject(String authSubject) {
        this.authSubject = authSubject;
    }

    public String getAuthAudience() {
        return authAudience;
    }

    public void setAuthAudience(String authAudience) {
        this.authAudience = authAudience;
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

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public int getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(int tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public int getWaitLength() {
        return waitLength;
    }

    public void setWaitLength(int waitLength) {
        this.waitLength = waitLength;
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

    public List<String> getAppliedPathPrefixes() {
        return appliedPathPrefixes;
    }

    public void setAppliedPathPrefixes(List<String> appliedPathPrefixes) {
        this.appliedPathPrefixes = appliedPathPrefixes;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null && (Boolean) object) {
            setEnabled(true);
        }
        object = mappedConfig.get(TOKEN_URL);
        if(object != null) {
            setTokenUrl((String) object);
        }
        object = mappedConfig.get(AUTH_ISSUER);
        if(object != null) {
            setAuthIssuer((String) object);
        }
        object = mappedConfig.get(AUTH_SUBJECT);
        if(object != null) {
            setAuthSubject((String) object);
        }
        object = mappedConfig.get(AUTH_AUDIENCE);
        if(object != null) {
            setAuthAudience((String) object);
        }
        object = mappedConfig.get(CERT_FILENAME);
        if(object != null) {
            setCertFilename((String) object);
        }
        object = mappedConfig.get(CERT_PASSWORD);
        if(object != null) {
            setCertPassword((String) object);
        }
        object = mappedConfig.get(IV);
        if(object != null) {
            setIv((String) object);
        }
        object = mappedConfig.get(TOKEN_TTL);
        if (object != null) {
            setTokenTtl((int) object);
        }
        object = mappedConfig.get(WAIT_LENGTH);
        if (object != null) {
            setWaitLength((int) object);
        }
        object = mappedConfig.get(PROXY_HOST);
        if(object != null) {
            setProxyHost((String) object);
        }
        object = mappedConfig.get(PROXY_PORT);
        if (object != null) {
            setProxyPort((int) object);
        }
        object = mappedConfig.get(ENABLE_HTTP2);
        if(object != null && (Boolean) object) {
            setEnableHttp2(true);
        }
        object = mappedConfig.get(SERVICE_HOST);
        if(object != null) {
            setServiceHost((String) object);
        }
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
