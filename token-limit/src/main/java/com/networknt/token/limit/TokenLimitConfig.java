package com.networknt.token.limit;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.JsonMapper;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

@ConfigSchema(configName = "token-limit", configKey = "token-limit", outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML})
public class TokenLimitConfig {
    private static final Logger logger = LoggerFactory.getLogger(TokenLimitConfig.class);
    public static final String CONFIG_NAME = "token-limit";
    ;
    private static final String ENABLED = "enabled";
    private static final String ERROR_ON_LIMIT = "errorOnLimit";
    private static final String DUPLICATE_LIMIT = "duplicateLimit";
    private static final String TOKEN_PATH_TEMPLATES = "tokenPathTemplates";
    private static final String LEGACY_CLIENT = "legacyClient";
    private static final String EXPIRE_KEY = "expireKey";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            description = "indicate if the handler is enabled or not. By default, it is enabled",
            defaultValue = "true"
    )
    @JsonProperty(ENABLED)
    Boolean enabled = true;

    @BooleanField(
            configFieldName = ERROR_ON_LIMIT,
            externalizedKeyName = ERROR_ON_LIMIT,
            externalized = true,
            description = "return an error if limit is reached. It should be the default behavior on dev/sit/stg. For production,\n" +
                    "a warning message should be logged. Also, this handler can be disabled on production for performance.",
            defaultValue = "true"
    )
    @JsonProperty(ERROR_ON_LIMIT)
    Boolean errorOnLimit = true;

    @IntegerField(
            configFieldName = DUPLICATE_LIMIT,
            externalizedKeyName = DUPLICATE_LIMIT,
            externalized = true,
            description = "The max number of duplicated token requests. Once this number is passed, the limit is triggered. This\n" +
                    "number is set based on the number of client instances as each instance might get its token if there\n" +
                    "is no distributed cache. The duplicated tokens are calculated based on the local in memory cache per\n" +
                    "light-gateway or oauth-kafka instance. Note: cache.yml needs to be configured.",
            defaultValue = "2"
    )
    @JsonProperty(DUPLICATE_LIMIT)
    Integer duplicateLimit = 2;

    @ArrayField(
            configFieldName = TOKEN_PATH_TEMPLATES,
            externalizedKeyName = TOKEN_PATH_TEMPLATES,
            externalized = true,
            description = "Different OAuth 2.0 providers have different token request path. To make sure that this handler only\n" +
                    "applied to the token endpoint, we define a list of path templates here to ensure request path is matched.\n" +
                    "The following is an example with two different OAuth 2.0 providers in values.yml file.\n" +
                    "token-limit.tokenPathTemplates:\n" +
                    "  - /oauth2/(?<instanceId>[^/]+)/v1/token\n" +
                    "  - /oauth2/(?<instanceId>[^/]+)/token",
            items = String.class
    )
    @JsonProperty(TOKEN_PATH_TEMPLATES)
    List<String> tokenPathTemplates = null;

    @ArrayField(
            configFieldName = LEGACY_CLIENT,
            externalizedKeyName = LEGACY_CLIENT,
            externalized = true,
            description = "List of ClientID that should be treated as Legacy and thus excluded from the token limit rules.\n" +
                    "This should only be used by approved legacy clients. The client ID is case insensitive.\n" +
                    "token-limit.legacyClient:\n" +
                    "  - 5oa66u56irXiekTUF1d6\n" +
                    "  - 6oa66u56irXiekABC1d4",
            items = String.class
    )
    List<String> legacyClient = null;

    @StringField(
            configFieldName = EXPIRE_KEY,
            externalizedKeyName = EXPIRE_KEY,
            defaultValue = "expires_in",
            externalized = true,
            description = "Expire key field name for the token limit cache feature. This is used to parse the response from\n" +
                    "the Auth Server, extract the expire time of the token and update to account for the time drift.\n" +
                    "The default value is \"expires_in\" and unit is seconds. If there's no such field in the response,\n" +
                    "set this property as empty (\"\") so the handler will skip the parsing and return the unmodified\n" +
                    "response payload as it was cached."
    )
    @JsonProperty(EXPIRE_KEY)
    String expireKey = "expires_in";

    private Map<String, Object> mappedConfig;
    private final Config config;

    private TokenLimitConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private TokenLimitConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setTokenPathTemplatesList();
        setLegacyClientList();
    }

    public static TokenLimitConfig load() {
        return new TokenLimitConfig();
    }

    public static TokenLimitConfig load(String configName) {
        return new TokenLimitConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setTokenPathTemplatesList();
        setLegacyClientList();
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean isErrorOnLimit() {
        return errorOnLimit;
    }

    public void setErrorOnLimit(boolean errorOnLimit) {
        this.errorOnLimit = errorOnLimit;
    }

    public Integer getDuplicateLimit() {
        return duplicateLimit;
    }

    public void setDuplicateLimit(int duplicateLimit) {
        this.duplicateLimit = duplicateLimit;
    }

    public List<String> getTokenPathTemplates() {
        return tokenPathTemplates;
    }

    public void setTokenPathTemplates(List<String> tokenPathTemplates) {
        this.tokenPathTemplates = tokenPathTemplates;
    }

    public List<String> getLegacyClient() {
        return legacyClient;
    }

    public void setLegacyClient(List<String> legacyClient) {
        this.legacyClient = legacyClient;
    }

    Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public String getExpireKey() {
        return expireKey;
    }
    public void setExpireKey(String expireKey) {
        this.expireKey = expireKey;
    }

    private void setConfigData() {
        Object object = getMappedConfig().get(ENABLED);
        if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = getMappedConfig().get(ERROR_ON_LIMIT);
        if(object != null) errorOnLimit = Config.loadBooleanValue(ERROR_ON_LIMIT, object);
        object = mappedConfig.get(DUPLICATE_LIMIT);
        if (object != null) duplicateLimit = Config.loadIntegerValue(DUPLICATE_LIMIT, object);
        object = getMappedConfig().get(EXPIRE_KEY);
        if(object != null) expireKey = ((String) object);
    }

    private void setTokenPathTemplatesList() {
        if (mappedConfig != null && mappedConfig.get(TOKEN_PATH_TEMPLATES) != null) {
            Object object = mappedConfig.get(TOKEN_PATH_TEMPLATES);
            tokenPathTemplates = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = {}", s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        tokenPathTemplates = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the tokenPathTemplates json with a list of strings.");
                    }
                } else {
                    // comma separated
                    tokenPathTemplates = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                tokenPathTemplates = (List<String>) getMappedConfig().get(TOKEN_PATH_TEMPLATES);
            } else {
                throw new ConfigException("tokenPathTemplates must be a string or a list of strings.");
            }
        }

    }

    private void setLegacyClientList() {
        if (mappedConfig != null && mappedConfig.get(LEGACY_CLIENT) != null) {
            Object object = mappedConfig.get(LEGACY_CLIENT);
            legacyClient = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("s = {}", s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        legacyClient = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the Legacy Client json with a list of strings.");
                    }
                } else {
                    // comma separated
                    legacyClient = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                legacyClient = (List<String>) getMappedConfig().get(LEGACY_CLIENT);
            } else {
                throw new ConfigException("legacyClient must be a string or a list of strings.");
            }
        }

    }
}
