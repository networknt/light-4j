package com.networknt.mcp;

import com.networknt.config.Config;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;
import com.networknt.server.ModuleRegistry;

import java.util.Map;

/**
 * Configuration class for McpHandler.
 *
 * @author Steve Hu
 */
@ConfigSchema(
        configKey = "mcp-router",
        configName = "mcp-router",
        configDescription = "MCP Router Configuration",
        outputFormats = {OutputFormat.JSON_SCHEMA, OutputFormat.YAML, OutputFormat.CLOUD}
)
public class McpConfig {
    /**
     * Config name
     */
    public static final String CONFIG_NAME = "mcp-router";
    /**
     * Enabled
     */
    public static final String ENABLED = "enabled";
    /**
     * SSE Path
     */
    public static final String SSE_PATH = "ssePath";
    /**
     * Message Path
     */
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
        mappedConfig = Config.getInstance().getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    private McpConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Load config
     * @return McpConfig
     */
    public static McpConfig load() {
        return new McpConfig(CONFIG_NAME);
    }

    /**
     * Load config
     * @param configName config name
     * @return McpConfig
     */
    public static McpConfig load(String configName) {
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

    /**
     * is enabled
     * @return boolean
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * get sse path
     * @return String
     */
    public String getSsePath() {
        return ssePath;
    }

    /**
     * get message path
     * @return String
     */
    public String getMessagePath() {
        return messagePath;
    }

    /**
     * get mapped config
     * @return Map
     */
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }
}
