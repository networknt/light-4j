package com.networknt.proxy.salesforce;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.proxy.PathPrefixAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SalesforceConfig {
    private static final Logger logger = LoggerFactory.getLogger(SalesforceConfig.class);

    public static final String CONFIG_NAME = "salesforce";
    public static final String ENABLED = "enabled";
    public static final String PATH_PREFIX = "pathPrefix";
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

    public static final String GRANT_TYPE = "grant_type";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String CLIENT_ID = "clientId";
    public static final String CLIENT_SECRET = "clientSecret";
    public static final String RESPONSE_TYPE = "responseType";

    List<UrlRewriteRule> urlRewriteRules;
    public static final String PATH_PREFIX_AUTHS = "pathPrefixAuths";
    public static final String SERVICE_HOST = "serviceHost";

    boolean enabled;
    String certFilename;
    String certPassword;
    String proxyHost;
    int proxyPort;
    boolean enableHttp2;

    String grantType;
    String username;
    String password;
    String clientId;
    String clientSecret;
    String responseType;

    List<PathPrefixAuth> pathPrefixAuths;
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
        setUrlRewriteRules();
        setConfigList();
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
        setUrlRewriteRules();
        setConfigList();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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

    public List<PathPrefixAuth> getPathPrefixAuths() {
        return pathPrefixAuths;
    }

    public void setPathPrefixAuths(List<PathPrefixAuth> pathPrefixAuths) {
        this.pathPrefixAuths = pathPrefixAuths;
    }
    public List<UrlRewriteRule> getUrlRewriteRules() {
        return urlRewriteRules;
    }

    public void setUrlRewriteRules() {
        this.urlRewriteRules = new ArrayList<>();
        if (mappedConfig.get("urlRewriteRules") !=null && mappedConfig.get("urlRewriteRules") instanceof String) {
            urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule((String)mappedConfig.get("urlRewriteRules")));
        } else {
            List<String> rules = (List)mappedConfig.get("urlRewriteRules");
            if(rules != null) {
                for (String s : rules) {
                    urlRewriteRules.add(UrlRewriteRule.convertToUrlRewriteRule(s));
                }
            }
        }
    }
    public void setUrlRewriteRules(List<UrlRewriteRule> urlRewriteRules) {
        this.urlRewriteRules = urlRewriteRules;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if(object != null && (Boolean) object) {
            setEnabled(true);
        }
        object = mappedConfig.get(CERT_FILENAME);
        if(object != null) {
            setCertFilename((String) object);
        }
        object = mappedConfig.get(CERT_PASSWORD);
        if(object != null) {
            setCertPassword((String) object);
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
                        pathPrefixAuths = Config.getInstance().getMapper().readValue(s, new TypeReference<List<PathPrefixAuth>>() {});
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
                    PathPrefixAuth pathPrefixAuth = new PathPrefixAuth();
                    pathPrefixAuth.setPathPrefix((String)value.get(PATH_PREFIX));
                    pathPrefixAuth.setGrantType((String)value.get(GRANT_TYPE));
                    pathPrefixAuth.setAuthIssuer((String)value.get(AUTH_ISSUER));
                    pathPrefixAuth.setAuthSubject((String)value.get(AUTH_SUBJECT));
                    pathPrefixAuth.setAuthAudience((String)value.get(AUTH_AUDIENCE));
                    pathPrefixAuth.setIv((String)value.get(IV));
                    pathPrefixAuth.setServiceHost((String)value.get(SERVICE_HOST));
                    pathPrefixAuth.setTokenTtl(value.get(TOKEN_TTL) == null ? 60 : (Integer)value.get(TOKEN_TTL));
                    pathPrefixAuth.setWaitLength(value.get(WAIT_LENGTH) == null ? 5000 : (Integer)value.get(WAIT_LENGTH));
                    pathPrefixAuth.setTokenUrl((String)value.get(TOKEN_URL));
                    // grant type password
                    pathPrefixAuth.setUsername((String)value.get(USERNAME));
                    pathPrefixAuth.setPassword((String)value.get(PASSWORD));
                    pathPrefixAuth.setClientId((String)value.get(CLIENT_ID));
                    pathPrefixAuth.setClientSecret((String)value.get(CLIENT_SECRET));
                    pathPrefixAuth.setResponseType((String)value.get(RESPONSE_TYPE));
                    pathPrefixAuths.add(pathPrefixAuth);
                }
            } else {
                throw new ConfigException("pathPrefixAuth must be a list of string object map.");
            }
        }
    }
}
