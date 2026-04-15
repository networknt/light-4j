package com.networknt.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to extract masking rules from MCP Tool Schemas dynamically.
 */
public class McpMaskingUtils {
    private static final Logger logger = LoggerFactory.getLogger(McpMaskingUtils.class);

    // Cache holding parsed schemas per tool name
    private static final Map<String, Map<String, String>> schemaMaskingRulesCache = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Integer>> schemaTokenizationRulesCache = new ConcurrentHashMap<>();

    /**
     * Parses an MCP Tool input schema to build dynamic JsonPath rules based on the 'x-mask' extension.
     * @param toolName The name of the tool (used for caching)
     * @param inputSchema The JSON Schema string
     * @return Map of JsonPath to replacement expression (e.g., {"$.ssn": "^(.*)$"})
     */
    public static Map<String, String> getMaskingRulesFromSchema(String toolName, String inputSchema) {
        if (inputSchema == null || inputSchema.trim().isEmpty()) {
            return new HashMap<>();
        }

        return schemaMaskingRulesCache.computeIfAbsent(toolName, k -> {
            Map<String, String> rules = new HashMap<>();
            Map<String, Integer> tokenizeRules = new HashMap<>();
            try {
                JsonNode schemaNode = Config.getInstance().getMapper().readTree(inputSchema);
                traverseSchema(schemaNode, "$", rules, tokenizeRules);
                schemaTokenizationRulesCache.put(toolName, tokenizeRules);
            } catch (Exception e) {
                logger.error("Failed to parse input schema for tool: " + toolName, e);
            }
            return rules;
        });
    }

    public static Map<String, Integer> getTokenizationRulesFromSchema(String toolName, String inputSchema) {
        if (inputSchema == null || inputSchema.trim().isEmpty()) {
            return new HashMap<>();
        }

        // Ensure it's populated
        getMaskingRulesFromSchema(toolName, inputSchema);
        return schemaTokenizationRulesCache.getOrDefault(toolName, new HashMap<>());
    }

    private static void traverseSchema(JsonNode node, String currentPath, Map<String, String> maskRules, Map<String, Integer> tokenizeRules) {
        if (node == null || !node.isObject()) {
            return;
        }

        // Check if this node is marked for masking
        if (node.has("x-mask") && node.get("x-mask").asBoolean(false)) {
            String pattern = node.has("x-mask-pattern") ? node.get("x-mask-pattern").asText() : "^(.*)$";
            maskRules.put(currentPath, pattern);
        }

        // Check if this node is marked for tokenization
        if (node.has("x-tokenize") && node.get("x-tokenize").isInt()) {
            tokenizeRules.put(currentPath, node.get("x-tokenize").asInt());
        }

        // If this is an object schema, its properties are in the "properties" field
        if (node.has("properties") && node.get("properties").isObject()) {
            JsonNode properties = node.get("properties");
            Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                traverseSchema(field.getValue(), currentPath + "." + field.getKey(), maskRules, tokenizeRules);
            }
        }

        // If this is an array schema, its items are in the "items" field
        if (node.has("items") && node.get("items").isObject()) {
            traverseSchema(node.get("items"), currentPath + "[*]", maskRules, tokenizeRules);
        }
    }
}
