package com.networknt.ldap;

import com.networknt.config.Config;

import java.util.Map;

public class LdapConfig {
    public static final String CONFIG_NAME = "ldap";
    public static final String URI = "uri";
    public static final String DOMAIN = "domain";
    public static final String PRINCIPAL = "principal";
    public static final String CREDENTIAL = "credential";
    public static final String SEARCH_FILTER = "searchFilter";
    public static final String SEARCH_BASE = "searchBase";

    String uri;
    String domain;
    String principal;
    String credential;
    String searchFilter;
    String searchBase;

    private final Config config;
    private Map<String, Object> mappedConfig;

    private LdapConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private LdapConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static LdapConfig load() {
        return new LdapConfig();
    }

    public static LdapConfig load(String configName) {
        return new LdapConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getDomain() { return domain; }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal;
    }

    public String getCredential() {
        return credential;
    }

    public void setCredential(String credential) {
        this.credential = credential;
    }

    public String getSearchFilter() { return searchFilter; }

    public void setSearchFilter(String searchFilter) { this.searchFilter = searchFilter; }

    public String getSearchBase() { return searchBase; }

    public void setSearchBase(String searchBase) { this.searchBase = searchBase; }

    private void setConfigData() {
        Object object = mappedConfig.get(URI);
        if (object != null) uri = (String)object;
        object = mappedConfig.get(DOMAIN);
        if (object != null) domain = (String)object;
        object = mappedConfig.get(PRINCIPAL);
        if (object != null) principal = (String)object;
        object = mappedConfig.get(CREDENTIAL);
        if (object != null) credential = (String)object;
        object = mappedConfig.get(SEARCH_FILTER);
        if (object != null) searchFilter = (String)object;
        object = mappedConfig.get(SEARCH_BASE);
        if (object != null) searchBase = (String)object;
    }

}
