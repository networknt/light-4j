package com.networknt.mcp;

import java.util.Map;

public interface McpTool {
    String getName();
    String getDescription();
    String getInputSchema();
    Map<String, Object> execute(Map<String, Object> arguments);
}
