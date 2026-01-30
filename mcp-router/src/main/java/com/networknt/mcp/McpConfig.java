package com.networknt.mcp;

import com.networknt.config.Config;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;
import com.networknt.server.ModuleRegistry;

import java.util.Map;

@ConfigSchema(
        configKey = "mcp-router",
        configName = "mcp-router",
        configDescription = "MCP Router Configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class McpConfig {
    public static final String CONFIG_NAME = "mcp-router";
    public static final String ENABLED = "enabled";
    public static final String SSE_PATH = "ssePath";
    public static final String MESSAGE_PATH = "messagePath";

    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Enable MCP Router Handler",
            defaultValue = "true"
    )
    boolean enabled = true;

    @StringField(
            configFieldName = SSE_PATH,
            externalizedKeyName = SSE_PATH,
            description = "Path for MCP Server-Sent Events (SSE) endpoint.",
            defaultValue = "/mcp/sse"
    )
    String ssePath = "/mcp/sse";

    @StringField(
            configFieldName = MESSAGE_PATH,
            externalizedKeyName = MESSAGE_PATH,
            description = "Path for MCP JSON-RPC message endpoint.",
            defaultValue = "/mcp/message"
    )
    String messagePath = "/mcp/message";

    private final Map<String, Object> mappedConfig;
    private static volatile McpConfig instance;

    private McpConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
        setConfigData();
    }

    private McpConfig() {
        this(CONFIG_NAME);
    }

    public static McpConfig load() {
        return load(CONFIG_NAME);
    }

    public static McpConfig load(String configName) {
        if (CONFIG_NAME.equals(configName)) {
            Map<String, Object> mappedConfig = Config.getInstance().getJsonMapConfig(configName);
            if (instance != null && instance.getMappedConfig() == mappedConfig) {
                return instance;
            }
            synchronized (McpConfig.class) {
                mappedConfig = Config.getInstance().getJsonMapConfig(configName);
                if (instance != null && instance.getMappedConfig() == mappedConfig) {
                    return instance;
                }
                instance = new McpConfig(configName);
                // Register the module with the configuration.
                ModuleRegistry.registerModule(configName, McpConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new McpConfig(configName);
    }

    private void setConfigData() {
        if (mappedConfig != null) {
            Object object = mappedConfig.get(ENABLED);
            if (object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            object = mappedConfig.get(SSE_PATH);
            if (object != null) ssePath = (String) object;
            object = mappedConfig.get(MESSAGE_PATH);
            if (object != null) messagePath = (String) object;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getSsePath() {
        return ssePath;
    }

    public String getMessagePath() {
        return messagePath;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }
}
