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
        
        // Assert rules mapping
        List<Map<String, Object>> rules = config.getRules();
        Assertions.assertNotNull(rules, "rules list should not be null");
        Assertions.assertEquals(1, rules.size(), "rules list should have 1 item");
        Assertions.assertEquals("/v1/test@post", rules.get(0).get("endpoint"));
        Assertions.assertEquals("test-rule-id", rules.get(0).get("ruleId"));
    }
}
