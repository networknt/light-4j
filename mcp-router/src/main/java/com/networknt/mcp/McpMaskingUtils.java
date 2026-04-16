package com.networknt.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.networknt.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility to extract masking rules from MCP Tool Schemas dynamically.
 */
public class McpMaskingUtils {
    private static final Logger logger = LoggerFactory.getLogger(McpMaskingUtils.class);
    private static final String X_TOKENIZE = "x-tokenize";
    private static final String PROPERTIES = "properties";
    private static final String ITEMS = "items";

    // Cache holding parsed schemas per tool name + schema fingerprint
    private static final Map<String, Map<String, String>> schemaMaskingRulesCache = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, Integer>> schemaTokenizationRulesCache = new ConcurrentHashMap<>();

    private McpMaskingUtils() {
    }

    /**
     * Parses an MCP Tool input schema to build dynamic JsonPath rules based on the 'x-mask' extension.
     * @param toolName The name of the tool (used for caching)
     * @param inputSchema The JSON Schema string
     * @return Map of JsonPath to replacement expression (e.g., {"$.ssn": "^(.*)$"})
     */
    public static Map<String, String> getMaskingRulesFromSchema(String toolName, String inputSchema) {
        if (toolName == null || toolName.trim().isEmpty() || inputSchema == null || inputSchema.trim().isEmpty()) {
            return new HashMap<>();
        }

        String cacheKey = getCacheKey(toolName, inputSchema);
        return schemaMaskingRulesCache.computeIfAbsent(cacheKey, k -> {
            Map<String, String> rules = new HashMap<>();
            Map<String, Integer> tokenizeRules = new HashMap<>();
            try {
                JsonNode schemaNode = Config.getInstance().getMapper().readTree(inputSchema);
                traverseSchema(schemaNode, "$", rules, tokenizeRules);
                schemaTokenizationRulesCache.put(cacheKey, tokenizeRules);
            } catch (Exception e) {
                logger.error("Failed to parse input schema for tool: " + toolName, e);
            }
            return rules;
        });
    }

    public static Map<String, Integer> getTokenizationRulesFromSchema(String toolName, String inputSchema) {
        if (toolName == null || toolName.trim().isEmpty() || inputSchema == null || inputSchema.trim().isEmpty()) {
            return new HashMap<>();
        }

        String cacheKey = getCacheKey(toolName, inputSchema);
        // Ensure it's populated
        getMaskingRulesFromSchema(toolName, inputSchema);
        return schemaTokenizationRulesCache.getOrDefault(cacheKey, new HashMap<>());
    }

    private static String getCacheKey(String toolName, String inputSchema) {
        return toolName + ":" + Objects.hashCode(inputSchema);
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
        if (node.has(X_TOKENIZE) && node.get(X_TOKENIZE).isInt()) {
            tokenizeRules.put(currentPath, node.get(X_TOKENIZE).asInt());
        }

        // If this is an object schema, its properties are in the "properties" field
        if (node.has(PROPERTIES) && node.get(PROPERTIES).isObject()) {
            JsonNode properties = node.get(PROPERTIES);
            Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                traverseSchema(field.getValue(), currentPath + "." + field.getKey(), maskRules, tokenizeRules);
            }
        }

        // If this is an array schema, its items are in the "items" field
        if (node.has(ITEMS) && node.get(ITEMS).isObject()) {
            traverseSchema(node.get(ITEMS), currentPath + "[*]", maskRules, tokenizeRules);
        }
    }
}
