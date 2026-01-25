package com.networknt.apikey;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.server.ModuleRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@ConfigSchema(
        configKey = "apikey",
        configName = "apikey",
        configDescription = "ApiKey Authentication Security Configuration for light-4j",
        outputFormats = {
                OutputFormat.JSON_SCHEMA,
                OutputFormat.YAML,
                OutputFormat.CLOUD
        }
)
public class ApiKeyConfig {
    private static final Logger logger = LoggerFactory.getLogger(ApiKeyConfig.class);

    public static final String CONFIG_NAME = "apikey";
    public static final String ENABLED = "enabled";
    public static final String HASH_ENABLED = "hashEnabled";
    public static final String PATH_PREFIX = "pathPrefix";
    public static final String HEADER_NAME = "headerName";
    public static final String API_KEY = "apiKey";
    public static final String PATH_PREFIX_AUTHS = "pathPrefixAuths";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            defaultValue = "true",
            description = "Enable ApiKey Authentication Handler, default is false."
    )
    boolean enabled;

    @BooleanField(
            configFieldName = HASH_ENABLED,
            externalizedKeyName = HASH_ENABLED,
            defaultValue = "false",
            description = """
                          If API key hash is enabled. The API key will be hashed with PBKDF2WithHmacSHA1 before it is
                          stored in the config file. It is more secure than put the encrypted key into the config file.
                          The default value is false. If you want to enable it, you need to use the following repo
                          https://github.com/networknt/light-hash command line tool to hash the clear text key."""
    )
    boolean hashEnabled;

    @ArrayField(
            configFieldName = PATH_PREFIX_AUTHS,
            externalizedKeyName = PATH_PREFIX_AUTHS,
            items = ApiKey.class,
            description = """
                    path prefix to the api key mapping. It is a list of map between the path prefix and the api key
                    for apikey authentication. In the handler, it loops through the list and find the matching path
                    prefix. Once found, it will check if the apikey is equal to allow the access or return an error.
                    The map object has three properties: pathPrefix, headerName and apiKey. Take a look at the test
                    resources/config folder for configuration examples.
                    """
    )
    List<ApiKey> pathPrefixAuths;

    private final Config config;
    private final Map<String, Object> mappedConfig;
    private static ApiKeyConfig instance;

    private ApiKeyConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfig(configName);
        setConfigData();
    }

    private ApiKeyConfig() {
        this(CONFIG_NAME);
    }

    public static ApiKeyConfig load() {
        return load(CONFIG_NAME);
    }

    public static ApiKeyConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (ApiKeyConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new ApiKeyConfig(configName);
                // Register the module with the configuration. masking the apiKey property.
                // As apiKeys are in the config file, we need to mask them.
                List<String> masks = new ArrayList<>();
                // if hashEnabled, there is no need to mask in the first place.
                if(!instance.hashEnabled) {
                    masks.add("apiKey");
                }
                ModuleRegistry.registerModule(configName, ApiKeyConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), masks);
                return instance;
            }
        }
        return new ApiKeyConfig(configName);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isHashEnabled() {
        return hashEnabled;
    }

    public void setHashEnabled(boolean hashEnabled) {
        this.hashEnabled = hashEnabled;
    }

    public List<ApiKey> getPathPrefixAuths() {
        return pathPrefixAuths;
    }

    public void setPathPrefixAuths(List<ApiKey> pathPrefixAuths) {
        this.pathPrefixAuths = pathPrefixAuths;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    Config getConfig() {
        return config;
    }

    private void setConfigData() {
        if (mappedConfig != null) {
            Object object = mappedConfig.get(ENABLED);
            if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            object = mappedConfig.get(HASH_ENABLED);
            if (object != null) hashEnabled = Config.loadBooleanValue(HASH_ENABLED, object);
            setConfigList();
        }
    }

    private void setConfigList() {
        // path prefix auth mapping
        if (mappedConfig.get(PATH_PREFIX_AUTHS) != null) {
            Object object = mappedConfig.get(PATH_PREFIX_AUTHS);
            pathPrefixAuths = new ArrayList<>();
            switch (object) {
                case String jsonList -> {
                    jsonList = jsonList.trim();
                    logger.trace("pathPrefixAuth s = {}", jsonList);
                    if (jsonList.startsWith("[")) {
                        // json format
                        try {
                            List<Map<String, Object>> values = Config.getInstance().getMapper().readValue(jsonList, new TypeReference<>() {
                            });
                            pathPrefixAuths = populatePathPrefixAuths(values);
                        } catch (Exception e) {
                            logger.error("Exception:", e);
                            throw new ConfigException("could not parse the pathPrefixAuth json with a list of string and object.");
                        }
                    } else {
                        throw new ConfigException("pathPrefixAuth must be a list of string object map.");
                    }
                }
                case List<?> authList when authList.stream().allMatch(item -> item instanceof Map<?, ?>) -> {
                    // the object is a list of map, we need convert it to PathPrefixAuth object.
                    @SuppressWarnings("unchecked") final var castAuthList = (List<Map<String, Object>>) authList;
                    pathPrefixAuths = populatePathPrefixAuths(castAuthList);
                }
                default -> throw new ConfigException("pathPrefixAuth must be a list of string object map.");
            }
        }
    }

    public static List<ApiKey> populatePathPrefixAuths(List<Map<String, Object>> values) {
        List<ApiKey> pathPrefixAuths = new ArrayList<>();
        for (Map<String, Object> value : values) {
            ApiKey apiKey = new ApiKey();
            apiKey.setPathPrefix((String) value.get(PATH_PREFIX));
            apiKey.setHeaderName((String) value.get(HEADER_NAME));
            apiKey.setApiKey((String) value.get(API_KEY));
            pathPrefixAuths.add(apiKey);
        }
        return pathPrefixAuths;
    }
}
