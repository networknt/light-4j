package com.networknt.proxy.salesforce;

import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import io.undertow.server.handlers.PathTemplateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SalesforceConfig {
    private static final Logger logger = LoggerFactory.getLogger(SalesforceConfig.class);

    public static final String CONFIG_NAME = "salesforce";
    public static final String ENABLED = "enabled";
    public static final String TOKEN_URL = "tokenUrl";
    public static final String AUTH_ISSUER = "authIssuer";
    public static final String AUTH_SUBJECT = "authSubject";
    public static final String AUTH_AUDIENCE = "authAudience";
    public static final String CERT_FILENAME = "certFilename";
    public static final String CERT_PASSWORD = "certPassword";
    public static final String IV = "iv";
    public static final String TOKEN_TTL = "tokenTtl";
    public static final String WAIT_LENGTH = "waitLength";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String ENABLE_HTTP2 = "enableHttps";
    public static final String PATH_PREFIX_AUTH = "pathPrefixAuth";
    public static final String MORNING_STAR = "morningStar";
    public static final String CONQUEST = "conquest";
    public static final String ADVISOR_HUB = "advisorHub";
    public static final String SERVICE_HOST = "serviceHost";

    boolean enabled;
    String tokenUrl;
    String authAudience;
    String certFilename;
    String certPassword;
    int tokenTtl;
    int waitLength;
    String proxyHost;
    int proxyPort;
    boolean enableHttp2;
    Map<String, Object> pathPrefixAuth;
    Map<String, Object> morningStar;
    Map<String, Object> conquest;
    Map<String, Object> advisorHub;

    String serviceHost;
    private Config config;
    private Map<String, Object> mappedConfig;

    private SalesforceConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private SalesforceConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigMap();
    }
    public static SalesforceConfig load() {
        return new SalesforceConfig();
    }

    public static SalesforceConfig load(String configName) {
        return new SalesforceConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigMap();
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

    public Map<String, Object> getPathPrefixAuth() {
        return pathPrefixAuth;
    }

    public void setPathPrefixAuth(Map<String, Object> pathPrefixAuth) {
        this.pathPrefixAuth = pathPrefixAuth;
    }
    public Map<String, Object> getMorningStar() { return morningStar; }
    public Map<String, Object> getConquest() { return conquest; }
    public Map<String, Object> getAdvisorHub() { return advisorHub; }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null && (Boolean) object) {
            setEnabled(true);
        }
        object = mappedConfig.get(TOKEN_URL);
        if(object != null) {
            setTokenUrl((String) object);
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

    private void setConfigMap() {
        // path prefix auth mapping
        if (mappedConfig.get(PATH_PREFIX_AUTH) != null) {
            Object object = mappedConfig.get(PATH_PREFIX_AUTH);
            pathPrefixAuth = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("pathPrefixAuth s = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        pathPrefixAuth = JsonMapper.string2Map(s);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the pathPrefixAuth json with a map of string and object.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        pathPrefixAuth.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                pathPrefixAuth = (Map)object;
            } else {
                throw new ConfigException("pathPrefixAuth must be a string object map.");
            }
        }
        // MorningStar map
        if (mappedConfig.get(MORNING_STAR) != null) {
            Object object = mappedConfig.get(MORNING_STAR);
            morningStar = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("morningStar s = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        morningStar = JsonMapper.string2Map(s);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the morningStar json with a map of string and object.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        morningStar.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                morningStar = (Map)object;
            } else {
                throw new ConfigException("morningStar must be a string object map.");
            }
        }
        // Conquest map
        if (mappedConfig.get(CONQUEST) != null) {
            Object object = mappedConfig.get(CONQUEST);
            conquest = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("conquest s = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        conquest = JsonMapper.string2Map(s);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the conquest json with a map of string and object.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        conquest.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                conquest = (Map)object;
            } else {
                throw new ConfigException("conquest must be a string object map.");
            }
        }
        // AdvisorHub map
        if (mappedConfig.get(ADVISOR_HUB) != null) {
            Object object = mappedConfig.get(ADVISOR_HUB);
            advisorHub = new HashMap<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("advisorHub s = " + s);
                if(s.startsWith("{")) {
                    // json format
                    try {
                        advisorHub = JsonMapper.string2Map(s);
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the advisorHub json with a map of string and object.");
                    }
                } else {
                    // comma separated
                    String[] pairs = s.split(",");
                    for (int i = 0; i < pairs.length; i++) {
                        String pair = pairs[i];
                        String[] keyValue = pair.split(":");
                        advisorHub.put(keyValue[0], keyValue[1]);
                    }
                }
            } else if (object instanceof Map) {
                advisorHub = (Map)object;
            } else {
                throw new ConfigException("advisorHub must be a string object map.");
            }
        }
    }
}
