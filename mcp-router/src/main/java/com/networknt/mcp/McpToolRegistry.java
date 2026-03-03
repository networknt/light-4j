package com.networknt.mcp;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for available MCP Tools.
 *
 * @author Steve Hu
 */
public class McpToolRegistry {
    private static final Map<String, McpTool> tools = new ConcurrentHashMap<>();

    private McpToolRegistry() {}

    /**
     * Register a tool
     * @param tool McpTool
     */
    public static void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
    }

    /**
     * Get a tool by name
     * @param name tool name
     * @return McpTool
     */
    public static McpTool getTool(String name) {
        return tools.get(name);
    }

    /**
     * Get all tools
     * @return Map of tools
     */
    public static Map<String, McpTool> getTools() {
        return Collections.unmodifiableMap(tools);
    }

    /**
     * Clear all tools (for testing/reload)
     */
    public static void clear() {
        tools.clear();
    }
}
