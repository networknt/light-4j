package com.networknt.ldap;

import com.networknt.config.Config;
import com.networknt.config.schema.ConfigSchema; // REQUIRED IMPORT
import com.networknt.config.schema.OutputFormat; // REQUIRED IMPORT
import com.networknt.config.schema.StringField; // REQUIRED IMPORT
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Config class for LDAP server connection and search settings.
 */
// <<< REQUIRED ANNOTATION FOR SCHEMA GENERATION >>>
@ConfigSchema(
        configKey = "ldap",
        configName = "ldap",
        configDescription = "LDAP server connection and search settings.",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML}
)
public class LdapConfig {
    public static final Logger logger = LoggerFactory.getLogger(LdapConfig.class);

    public static final String CONFIG_NAME = "ldap";
    public static final String URI = "uri";
    public static final String DOMAIN = "domain";
    public static final String PRINCIPAL = "principal";
    public static final String CREDENTIAL = "credential";
    public static final String SEARCH_FILTER = "searchFilter";
    public static final String SEARCH_BASE = "searchBase";

    // --- Annotated Fields ---
    private final Config config;
    private Map<String, Object> mappedConfig;

    @StringField(
            configFieldName = URI,
            externalizedKeyName = URI,
            description = "The LDAP server uri.",
            externalized = true
    )
    String uri;

    @StringField(
            configFieldName = DOMAIN,
            externalizedKeyName = DOMAIN,
            description = "The LDAP domain name.",
            externalized = true
    )
    String domain;

    @StringField(
            configFieldName = PRINCIPAL,
            externalizedKeyName = PRINCIPAL,
            description = "The user principal for binding (authentication).",
            externalized = true
    )
    String principal;

    @StringField(
            configFieldName = CREDENTIAL,
            externalizedKeyName = CREDENTIAL,
            description = "The user credential (password) for binding.",
            externalized = true
    )
    String credential;

    @StringField(
            configFieldName = SEARCH_FILTER,
            externalizedKeyName = SEARCH_FILTER,
            description = "The search filter (e.g., (&(objectClass=user)(sAMAccountName={0}))).",
            externalized = true
    )
    String searchFilter;

    @StringField(
            configFieldName = SEARCH_BASE,
            externalizedKeyName = SEARCH_BASE,
            description = "The search base DN (Distinguished Name).",
            externalized = true
    )
    String searchBase;

    // --- Constructor and Loading Logic ---

    private LdapConfig() {
        this(CONFIG_NAME);
    }

    private LdapConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData(); // Custom logic for loading string fields
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

    // --- Getters and Setters (Original Methods) ---

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
