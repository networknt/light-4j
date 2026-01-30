package com.networknt.mcp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpToolRegistry {
    private static final Map<String, McpTool> tools = new ConcurrentHashMap<>();

    private McpToolRegistry() {}

    public static void registerTool(McpTool tool) {
        tools.put(tool.getName(), tool);
    }

    public static McpTool getTool(String name) {
        return tools.get(name);
    }

    public static Map<String, McpTool> getTools() {
        return tools;
    }
}
