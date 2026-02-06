package com.networknt.mcp;

import com.networknt.config.Config;
import com.networknt.config.schema.ArrayField;
import com.networknt.config.schema.BooleanField;
import com.networknt.config.schema.ConfigSchema;
import com.networknt.config.schema.OutputFormat;
import com.networknt.config.schema.StringField;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.ConfigException;
import java.util.ArrayList;

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
    private static final Logger logger = LoggerFactory.getLogger(McpConfig.class);
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

    @ArrayField(
            configFieldName = TOOLS,
            externalizedKeyName = TOOLS,
            items = Tool.class,
            description = "List of tools"
    )
    private List<Tool> tools;

    private final Map<String, Object> mappedConfig;
    private static volatile McpConfig instance;

    private McpConfig(String configName) {
        mappedConfig = Config.getInstance().getJsonMapConfig(configName);
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
        return load(CONFIG_NAME);
    }

    /**
     * Load config
     * @param configName config name
     * @return McpConfig
     */
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
                ModuleRegistry.registerModule(configName, McpConfig.class.getName(), Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(configName), null);
                return instance;
            }
        }
        return new McpConfig(configName);
    }

    private void setConfigData() {
        if (mappedConfig != null) {
            Object object = mappedConfig.get(ENABLED);
            if(object != null) enabled = Config.loadBooleanValue(ENABLED, object);
            object = mappedConfig.get(PATH);
            if (object != null) path = (String) object;
            setConfigList();
        }
    }

    private void setConfigList() {
        if (mappedConfig.get(TOOLS) != null) {
            Object object = mappedConfig.get(TOOLS);
            tools = new ArrayList<>();
            switch (object) {
                case String jsonList -> {
                    jsonList = jsonList.trim();
                    if (logger.isTraceEnabled()) logger.trace("tools s = {}", jsonList);
                    if (jsonList.startsWith("[")) {
                        // json format
                        try {
                            List<Map<String, Object>> values = Config.getInstance().getMapper().readValue(jsonList, new TypeReference<>() {
                            });
                            tools = populateTools(values);
                        } catch (Exception e) {
                            logger.error("Exception:", e);
                            throw new ConfigException("could not parse the tools json with a list of string and object.");
                        }
                    } else {
                        throw new ConfigException("tools must be a list of string object map.");
                    }
                }
                case List<?> toolList when toolList.stream().allMatch(item -> item instanceof Map<?, ?>) -> {
                    // the object is a list of map, we need convert it to Tool object.
                    @SuppressWarnings("unchecked") final var castToolList = (List<Map<String, Object>>) toolList;
                    tools = populateTools(castToolList);
                }
                default -> throw new ConfigException("tools must be a list of string object map.");
            }
        }
    }

    public static List<Tool> populateTools(List<Map<String, Object>> values) {
        List<Tool> tools = new ArrayList<>();
        for (Map<String, Object> value : values) {
            Tool tool = new Tool();
            tool.setName((String) value.get("name"));
            tool.setDescription((String) value.get("description"));
            tool.setHost((String) value.get("host"));
            tool.setPath((String) value.get("path"));
            tool.setMethod((String) value.get("method"));
            tool.setProtocol((String) value.get("protocol"));
            Object schemaObj = value.get("inputSchema");
            if (schemaObj != null) {
                if (schemaObj instanceof Map) {
                    try {
                        tool.setInputSchema(Config.getInstance().getMapper().writeValueAsString(schemaObj));
                    } catch (Exception e) {
                        logger.error("Failed to serialize inputSchema for tool " + tool.getName(), e);
                    }
                } else if (schemaObj instanceof String) {
                    tool.setInputSchema((String) schemaObj);
                }
            }
            tools.add(tool);
        }
        return tools;
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
    public List<Tool> getTools() {
        return tools;
    }
}
