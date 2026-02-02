package com.networknt.mcp;

import com.networknt.config.Config;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;
import com.networknt.server.ModuleRegistry;

import java.util.Map;
import java.util.List;

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
     * Path
     */
    public static final String PATH = "path";

    /**
     * Tools
     */
    public static final String TOOLS = "tools";
    
    @BooleanField(
            configFieldName = ENABLED,
            externalizedKeyName = ENABLED,
            description = "Enable MCP Router Handler",
            defaultValue = "true"
    )
    boolean enabled = true;

    @StringField(
            configFieldName = PATH,
            externalizedKeyName = PATH,
            description = "Path for MCP endpoint (Streamable HTTP)",
            defaultValue = "/mcp"
    )
    String path = "/mcp";

    private List<Map<String, Object>> tools;

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
            if(object != null && (Boolean) object) {
                enabled = true;
            }
            object = mappedConfig.get(PATH);
            if (object != null) path = (String) object;
            object = mappedConfig.get(TOOLS);
            if(object != null && object instanceof List) {
                tools = (List<Map<String, Object>>) object;
            }
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
     * get path
     * @return String
     */
    public String getPath() {
        return path;
    }

    /**
     * get mapped config
     * @return Map
     */
    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    /**
     * get tools
     * @return List
     */
    public List<Map<String, Object>> getTools() {
        return tools;
    }
}
