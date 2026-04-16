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
}
