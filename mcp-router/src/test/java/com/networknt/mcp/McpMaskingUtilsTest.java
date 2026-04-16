package com.networknt.mcp;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpMaskingUtilsTest {
    @Test
    void shouldReturnEmptyMaskingRulesWhenToolNameIsNull() {
        Map<String, String> rules = McpMaskingUtils.getMaskingRulesFromSchema(null, "{\"type\":\"object\"}");
        assertTrue(rules.isEmpty());
    }

    @Test
    void shouldReturnEmptyTokenizationRulesWhenToolNameIsBlank() {
        Map<String, Integer> rules = McpMaskingUtils.getTokenizationRulesFromSchema("  ", "{\"type\":\"object\"}");
        assertEquals(0, rules.size());
    }

    @Test
    void shouldRefreshRulesWhenSchemaChangesForSameTool() {
        String toolName = "tool1";
        String schemaWithMask = """
                {"type":"object","properties":{"ssn":{"type":"string","x-mask":true,"x-mask-pattern":"^(.*)$"}}}
                """;
        String schemaWithTokenize = "{\"type\":\"object\",\"properties\":{\"ssn\":{\"type\":\"string\",\"x-tokenize\":1}}}";

        Map<String, String> maskingRules = McpMaskingUtils.getMaskingRulesFromSchema(toolName, schemaWithMask);
        assertEquals(1, maskingRules.size());

        Map<String, String> changedMaskingRules = McpMaskingUtils.getMaskingRulesFromSchema(toolName, schemaWithTokenize);
        assertTrue(changedMaskingRules.isEmpty());

        Map<String, Integer> tokenizationRules = McpMaskingUtils.getTokenizationRulesFromSchema(toolName, schemaWithTokenize);
        assertEquals(1, tokenizationRules.size());
        assertEquals(1, tokenizationRules.get("$.ssn"));
    }
}
