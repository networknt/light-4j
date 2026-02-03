package com.networknt.mcp;

import java.util.Map;

/**
 * McpTool interface.
 *
 * @author Steve Hu
 */
public interface McpTool {
    /**
     * Get tool name
     * @return String
     */
    String getName();

    /**
     * Get tool description
     * @return String
     */
    String getDescription();

    /**
     * Get input schema
     * @return String
     */
    String getInputSchema();

    /**
     * Execute the tool
     * @param arguments map of arguments
     * @return map of result
     */
    Map<String, Object> execute(Map<String, Object> arguments);
}
