package com.networknt.router;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@ConfigSchema(
        configKey = "oauthServer",
        configName = "oauthServer",
        configDescription = "OAuth server configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class OAuthServerConfig {
    private static final Logger logger = LoggerFactory.getLogger(OAuthServerConfig.class);
    public static final String CONFIG_NAME = "oauthServer";

    private static final String ENABLED = "enabled";
    private static final String GET_METHOD_ENABLED = "getMethodEnabled";
    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final String PASS_THROUGH = "passThrough";
    private static final String TOKEN_SERVICE_ID = "tokenServiceId";
    private Map<String, Object> mappedConfig;
    private final Config config;

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            externalized = true,
            defaultValue = "true",
            description = "indicate if the handler is enabled or not in the handler chain."
    )
    private boolean enabled;

    @BooleanField(
            configFieldName = GET_METHOD_ENABLED,
            externalizedKeyName = GET_METHOD_ENABLED,
            externalized = true,
            defaultValue = "false",
            description = "If the handler supports get request. This is a feature that is only used for consumers migrated from the SAG gateway as\n" +
                    "a temporary solution. It shouldn't be used in the new development as all credentials are revealed in the URL."
    )
    private boolean getMethodEnabled;

    @ArrayField(
            configFieldName = CLIENT_CREDENTIALS,
            externalizedKeyName = CLIENT_CREDENTIALS,
            externalized = true,
            description = "A list of client_id and client_secret concat with a colon.",
            items = String.class
    )
    List<String> clientCredentials;

    @BooleanField(
            configFieldName = PASS_THROUGH,
            externalizedKeyName = PASS_THROUGH,
            externalized = true,
            defaultValue = "false",
            description = "An indicator to for path through to an OAuth 2.0 server to get a real token."
    )
    private boolean passThrough;

    @StringField(
            configFieldName = TOKEN_SERVICE_ID,
            externalizedKeyName = TOKEN_SERVICE_ID,
            externalized = true,
            defaultValue = "light-proxy-client",
            description = "If pathThrough is set to true, this is the serviceId that is used in the client.yml configuration as the key\n" +
                    "to get all the properties to connect to the target OAuth 2.0 provider to get client_credentials access token.\n" +
                    "The client.yml must be set to true for multipleAuthServers and the token will be verified on the same LPC."
    )
    private String tokenServiceId;

    private OAuthServerConfig() {
        this(CONFIG_NAME);
    }

    private OAuthServerConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }

    public static OAuthServerConfig load() {
        return new OAuthServerConfig();
    }

    public static OAuthServerConfig load(String configName) {
        return new OAuthServerConfig(configName);
    }

    public void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }


    public boolean isEnabled() {
        return enabled;
    }

    public boolean isGetMethodEnabled() {
        return getMethodEnabled;
    }

    public List<String> getClientCredentials() {
        return clientCredentials;
    }

    public boolean isPassThrough() {
        return passThrough;
    }

    public void setPassThrough(boolean passThrough) {
        this.passThrough = passThrough;
    }

    public String getTokenServiceId() {
        return tokenServiceId;
    }

    public void setTokenServiceId(String tokenServiceId) {
        this.tokenServiceId = tokenServiceId;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(ENABLED);
        if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
        object = mappedConfig.get(GET_METHOD_ENABLED);
        if (object != null) getMethodEnabled = Config.loadBooleanValue(GET_METHOD_ENABLED, object);
        object = mappedConfig.get(PASS_THROUGH);
        if (object != null) passThrough = Config.loadBooleanValue(PASS_THROUGH, object);
        tokenServiceId = (String) getMappedConfig().get(TOKEN_SERVICE_ID);
    }

    private void setConfigList() {
        if (mappedConfig.get(CLIENT_CREDENTIALS) != null) {
            Object object = mappedConfig.get(CLIENT_CREDENTIALS);
            clientCredentials = new ArrayList<>();
            if (object instanceof String) {
                String s = (String) object;
                s = s.trim();
                if (logger.isTraceEnabled()) logger.trace("s = " + s);
                if (s.startsWith("[")) {
                    // json format
                    try {
                        clientCredentials = Config.getInstance().getMapper().readValue(s, new TypeReference<List<String>>() {
                        });
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the clientCredentials json with a list of strings.");
                    }
                } else {
                    // comma separated
                    clientCredentials = Arrays.asList(s.split("\\s*,\\s*"));
                }
            } else if (object instanceof List) {
                List prefixes = (List) object;
                prefixes.forEach(item -> {
                    clientCredentials.add((String) item);
                });
            } else {
                throw new ConfigException("clientCredentials must be a string or a list of strings.");
            }
        }
    }
}
