package com.networknt.rule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class RuleConfigTest {

    @Test
    public void testRuleConfigMapping() {
        RuleConfig config = RuleConfig.load();
        
        // Assert ruleBodies mapping
        Map<String, Object> ruleBodies = config.getRuleBodies();
        Assertions.assertNotNull(ruleBodies, "ruleBodies should not be null");
        Assertions.assertTrue(ruleBodies.containsKey("test-rule-id"), "ruleBodies should contain test-rule-id");
        
        Map<String, Object> ruleBody = (Map<String, Object>) ruleBodies.get("test-rule-id");
        Assertions.assertEquals("req-acc", ruleBody.get("ruleType"));
        
        // Assert endpointRules mapping
        Map<String, Object> endpointRules = config.getEndpointRules();
        Assertions.assertNotNull(endpointRules, "endpointRules map should not be null");
        Assertions.assertEquals(1, endpointRules.size(), "endpointRules map should have 1 item");
        Assertions.assertTrue(endpointRules.containsKey("/v1/test@post"), "endpointRules should contain /v1/test@post");
        
        Map<String, Object> endpointRule = (Map<String, Object>) endpointRules.get("/v1/test@post");
        List<String> ruleIds = (List<String>) endpointRule.get("req-acc");
        Assertions.assertNotNull(ruleIds, "ruleIds for req-acc should not be null");
        Assertions.assertEquals("test-rule-id", ruleIds.get(0));
    }
}
