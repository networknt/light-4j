package com.networknt.rule;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;

/**
 * MultiThreadRuleExecutor test class.
 *
 * @author Steve Hu
 */
public class MultiThreadRuleExecutorTest {

    @Test
    public void testParallelExecution() throws Exception {
        RuleEngine engine = Mockito.mock(RuleEngine.class);

        Rule r1 = new Rule();
        r1.setRuleId("r1");
        Rule r2 = new Rule();
        r2.setRuleId("r2");

        Map<String, Rule> rules = new HashMap<>();
        rules.put("r1", r1);
        rules.put("r2", r2);

        Map<String, Object> res1 = new HashMap<>();
        res1.put(RuleConstants.RESULT, true);
        res1.put("key1", "val1");

        Map<String, Object> res2 = new HashMap<>();
        res2.put(RuleConstants.RESULT, true);
        res2.put("key2", "val2");

        Mockito.when(engine.executeRule(eq("r1"), anyMap())).thenReturn(res1);
        Mockito.when(engine.executeRule(eq("r2"), anyMap())).thenReturn(res2);

        MultiThreadRuleExecutor executor = new MultiThreadRuleExecutor(rules, engine);
        Map<String, Object> input = new HashMap<>();
        input.put("input", "test");

        List<String> ruleIds = List.of("r1", "r2");
        Map<String, Object> result = executor.executeRules(ruleIds, "parallel", input);

        Assertions.assertTrue((Boolean) result.get(RuleConstants.RESULT));
        Assertions.assertEquals("val1", result.get("key1"));
        Assertions.assertEquals("val2", result.get("key2"));

        Mockito.verify(engine, Mockito.times(1)).executeRule(eq("r1"), anyMap());
        Mockito.verify(engine, Mockito.times(1)).executeRule(eq("r2"), anyMap());
    }

    @Test
    public void testParallelExecutionOneFails() throws Exception {
        RuleEngine engine = Mockito.mock(RuleEngine.class);

        Rule r1 = new Rule();
        r1.setRuleId("r1");
        Rule r2 = new Rule();
        r2.setRuleId("r2");

        Map<String, Rule> rules = new HashMap<>();
        rules.put("r1", r1);
        rules.put("r2", r2);

        Map<String, Object> res1 = new HashMap<>();
        res1.put(RuleConstants.RESULT, false);

        Map<String, Object> res2 = new HashMap<>();
        res2.put(RuleConstants.RESULT, true);
        res2.put("key2", "val2");

        Mockito.when(engine.executeRule(eq("r1"), anyMap())).thenReturn(res1);
        Mockito.when(engine.executeRule(eq("r2"), anyMap())).thenReturn(res2);

        MultiThreadRuleExecutor executor = new MultiThreadRuleExecutor(rules, engine);
        List<String> ruleIds = List.of("r1", "r2");
        Map<String, Object> result = executor.executeRules(ruleIds, "parallel", new HashMap<>());

        Assertions.assertFalse((Boolean) result.get(RuleConstants.RESULT));
        Assertions.assertEquals("val2", result.get("key2"));
    }
}
