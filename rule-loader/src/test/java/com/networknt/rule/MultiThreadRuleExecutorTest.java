package com.networknt.rule;

import com.networknt.config.Config;
import com.networknt.rule.exception.RuleEngineException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collection;
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

    public static class LegacyTestAction implements IAction {
        @Override
        public void performAction(String ruleId, String actionId, Map<String, Object> objMap,
                                  Map<String, Object> resultMap, Collection<RuleActionValue> actionValues)
                throws RuleEngineException {
            resultMap.put("legacyActionId", actionId);
            resultMap.put("legacyResolvedValue", actionValues.iterator().next().getResolvedValue());
        }
    }

    @Test
    public void testLegacyRuleBodiesLoadAndExecuteAtStartup() throws Exception {
        Config config = Config.getInstance();
        Map<String, Object> originalRuleConfig = config.getJsonMapConfig(RuleConfig.CONFIG_NAME);

        try {
            config.putInConfigCache(RuleConfig.CONFIG_NAME, legacyConfig("legacy-startup", "/v1/startup@post"));
            setRuleConfigInstance(null);

            MultiThreadRuleExecutor executor = new MultiThreadRuleExecutor();

            assertCompleteLegacyRule(executor.getRules().get("legacy-startup"), "legacy-startup");
            Assertions.assertTrue(executor.getRuleEngine().actionClassCache.containsKey(LegacyTestAction.class.getName()),
                    "the legacy action class should be preloaded during startup");

            Map<String, Object> result = executor.executeRules(
                    "/v1/startup@post", "req-tra", new HashMap<>(Map.of("name", "expected")));

            Assertions.assertEquals(Boolean.TRUE, result.get(RuleConstants.RESULT));
            Assertions.assertEquals("legacy-action", result.get("legacyActionId"));
            Assertions.assertEquals("expected", result.get("legacyResolvedValue"));
        } finally {
            restoreRuleConfig(config, originalRuleConfig);
        }
    }

    @Test
    public void testCelRuleRejectsEntireRuleBodiesDocumentAtStartup() throws Exception {
        Config config = Config.getInstance();
        Map<String, Object> originalRuleConfig = config.getJsonMapConfig(RuleConfig.CONFIG_NAME);

        try {
            Map<String, Object> mixedRuleBodies = new HashMap<>();
            mixedRuleBodies.putAll((Map<String, Object>) legacyConfig(
                    "legacy-startup", "/v1/startup@post").get(RuleConfig.RULE_BODIES));
            mixedRuleBodies.putAll((Map<String, Object>) celConfig().get(RuleConfig.RULE_BODIES));
            config.putInConfigCache(RuleConfig.CONFIG_NAME, Map.of(
                    RuleConfig.RULE_BODIES, mixedRuleBodies,
                    RuleConfig.ENDPOINT_RULES, Map.of()));
            setRuleConfigInstance(null);

            RuntimeException exception = Assertions.assertThrows(
                    RuntimeException.class, MultiThreadRuleExecutor::new);
            Assertions.assertTrue(hasCause(exception, com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException.class));
        } finally {
            restoreRuleConfig(config, originalRuleConfig);
        }
    }

    @Test
    public void testLegacyRuleBodiesReloadAndExecute() throws Exception {
        Config config = Config.getInstance();
        Map<String, Object> originalRuleConfig = config.getJsonMapConfig(RuleConfig.CONFIG_NAME);

        try {
            config.putInConfigCache(RuleConfig.CONFIG_NAME, legacyConfig("legacy-before-reload", "/v1/before@post"));
            setRuleConfigInstance(null);
            MultiThreadRuleExecutor executor = new MultiThreadRuleExecutor();
            RuleEngine initialEngine = executor.getRuleEngine();

            config.putInConfigCache(RuleConfig.CONFIG_NAME, legacyConfig("legacy-after-reload", "/v1/after@post"));
            setRuleConfigInstance(null);

            Map<String, Object> result = executor.executeRules(
                    "/v1/after@post", "res-tra", new HashMap<>(Map.of("name", "expected")));

            Assertions.assertNotSame(initialEngine, executor.getRuleEngine(),
                    "configuration reload should rebuild the rule engine");
            Assertions.assertNull(executor.getRules().get("legacy-before-reload"));
            assertCompleteLegacyRule(executor.getRules().get("legacy-after-reload"), "legacy-after-reload");
            Assertions.assertTrue(executor.getRuleEngine().actionClassCache.containsKey(LegacyTestAction.class.getName()),
                    "the legacy action class should be preloaded after reload");
            Assertions.assertEquals(Boolean.TRUE, result.get(RuleConstants.RESULT));
            Assertions.assertEquals("legacy-action", result.get("legacyActionId"));
            Assertions.assertEquals("expected", result.get("legacyResolvedValue"));
        } finally {
            restoreRuleConfig(config, originalRuleConfig);
        }
    }

    @Test
    public void testInvalidCelReloadRetainsLastKnownGoodRulesWithoutRepeatedRequestFailure() throws Exception {
        Config config = Config.getInstance();
        Map<String, Object> originalRuleConfig = config.getJsonMapConfig(RuleConfig.CONFIG_NAME);

        try {
            config.putInConfigCache(RuleConfig.CONFIG_NAME, legacyConfig("legacy-active", "/v1/active@post"));
            setRuleConfigInstance(null);
            MultiThreadRuleExecutor executor = new MultiThreadRuleExecutor();
            RuleEngine activeEngine = executor.getRuleEngine();
            RuleConfig activeConfig = (RuleConfig) getField(executor, "config");

            config.putInConfigCache(RuleConfig.CONFIG_NAME, celConfig());
            setRuleConfigInstance(null);
            RuleConfig invalidConfig = RuleConfig.load();

            for (int request = 0; request < 2; request++) {
                Map<String, Object> result = executor.executeRules(
                        "/v1/active@post", "req-tra", new HashMap<>(Map.of("name", "expected")));
                Assertions.assertEquals(Boolean.TRUE, result.get(RuleConstants.RESULT));
                Assertions.assertEquals("legacy-action", result.get("legacyActionId"));
                Assertions.assertSame(activeEngine, executor.getRuleEngine());
            }

            Assertions.assertSame(activeConfig, getField(executor, "config"),
                    "an invalid reload must not replace the active configuration");
            Assertions.assertSame(invalidConfig, getField(executor, "rejectedConfig"),
                    "the rejected snapshot should be remembered so each request does not rebuild it");
            Assertions.assertNotNull(executor.getRules().get("legacy-active"));
            Assertions.assertNull(executor.getRules().get("cel-rule"));
        } finally {
            restoreRuleConfig(config, originalRuleConfig);
        }
    }

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

    @Test
    public void testReloadClearsStaleRulesWhenRuleBodiesRemoved() throws Exception {
        RuleEngine engine = Mockito.mock(RuleEngine.class);
        Map<String, Rule> rules = new HashMap<>();
        rules.put("r1", new Rule());

        MultiThreadRuleExecutor executor = new MultiThreadRuleExecutor(rules, engine);
        Config config = Config.getInstance();
        Map<String, Object> originalRuleConfig = config.getJsonMapConfig(RuleConfig.CONFIG_NAME);

        try {
            Map<String, Object> initialMappedConfig = new HashMap<>();
            initialMappedConfig.put(RuleConfig.ENDPOINT_RULES, Map.of("/v1/test@post", Map.of("req-acc", List.of("r1"))));
            config.putInConfigCache(RuleConfig.CONFIG_NAME, initialMappedConfig);
            setRuleConfigInstance(null);
            RuleConfig initialConfig = RuleConfig.load();
            setField(executor, "config", initialConfig);

            Map<String, Object> reloadedMappedConfig = new HashMap<>();
            reloadedMappedConfig.put(RuleConfig.ENDPOINT_RULES, new HashMap<>());
            config.putInConfigCache(RuleConfig.CONFIG_NAME, reloadedMappedConfig);
            setRuleConfigInstance(null);

            Map<String, Object> result = executor.executeRules("/v1/test@post", "req-acc", new HashMap<>());

            Assertions.assertNull(result);
            Assertions.assertNull(executor.getRules(), "rules should be cleared when ruleBodies are removed on reload");
            Assertions.assertNull(executor.getRuleEngine(), "ruleEngine should be cleared when ruleBodies are removed on reload");
            Assertions.assertTrue(executor.getEndpointRules().isEmpty(), "endpointRules should be updated from the reloaded config");
        } finally {
            if (originalRuleConfig != null) {
                config.putInConfigCache(RuleConfig.CONFIG_NAME, originalRuleConfig);
            } else {
                config.clearConfigCache(RuleConfig.CONFIG_NAME);
            }
            setRuleConfigInstance(null);
        }
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = MultiThreadRuleExecutor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object getField(Object target, String fieldName) throws Exception {
        Field field = MultiThreadRuleExecutor.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(target);
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    private static void setRuleConfigInstance(RuleConfig value) throws Exception {
        Field field = RuleConfig.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, value);
    }

    private static Map<String, Object> legacyConfig(String ruleId, String endpoint) {
        Map<String, Object> conditionValue = new HashMap<>();
        conditionValue.put("conditionValueId", "legacy-value");
        conditionValue.put("conditionValue", "expected");
        conditionValue.put("expression", false);
        conditionValue.put("valueTypeCode", "STRING");
        conditionValue.put("regexFlags", "CASE_INSENSITIVE");
        conditionValue.put("dateFormat", "yyyy-MM-dd");

        Map<String, Object> condition = new HashMap<>();
        condition.put("conditionId", "name-matches");
        condition.put("propertyPath", "name");
        condition.put("conditionDesc", "legacy condition");
        condition.put("operatorCode", "equals");
        condition.put("index", 0);
        condition.put("conditionValues", List.of(conditionValue));

        Map<String, Object> actionValue = new HashMap<>();
        actionValue.put("actionValueId", "legacy-action-value");
        actionValue.put("valueTypeCode", "STRING");
        actionValue.put("value", "${name}");

        Map<String, Object> action = new HashMap<>();
        action.put("actionId", "legacy-action");
        action.put("actionDesc", "legacy action");
        action.put("actionClassName", LegacyTestAction.class.getName());
        action.put("conditionResult", true);
        action.put("actionValues", List.of(actionValue));
        action.put("parameters", Map.of("source", "legacy-ruleBodies"));

        Map<String, Object> rule = new HashMap<>();
        rule.put("ruleId", ruleId);
        rule.put("hostId", "legacy-host");
        rule.put("ruleType", "generic");
        rule.put("ruleName", "Legacy loader compatibility");
        rule.put("ruleVersion", "2.0.1");
        rule.put("ruleGroup", "compatibility");
        rule.put("ruleDesc", "Complete legacy rule body");
        rule.put("ruleOwner", "light-4j");
        rule.put("common", "Y");
        rule.put("conditions", List.of(condition));
        rule.put("conditionExpression", "name-matches");
        rule.put("actions", List.of(action));

        return Map.of(
                RuleConfig.RULE_BODIES, Map.of(ruleId, rule),
                RuleConfig.ENDPOINT_RULES, Map.of(endpoint, Map.of(
                        "req-tra", List.of(ruleId),
                        "res-tra", List.of(ruleId))));
    }

    private static Map<String, Object> celConfig() {
        Map<String, Object> rule = new HashMap<>();
        rule.put("ruleId", "cel-rule");
        rule.put("ruleType", "request-access");
        rule.put("conditionLanguage", "cel");
        rule.put("expression", "request.path == '/admin'");

        return Map.of(
                RuleConfig.RULE_BODIES, Map.of("cel-rule", rule),
                RuleConfig.ENDPOINT_RULES, Map.of(
                        "/v1/admin@get", Map.of("req-acc", List.of("cel-rule"))));
    }

    private static void assertCompleteLegacyRule(Rule rule, String ruleId) {
        Assertions.assertNotNull(rule);
        Assertions.assertEquals(ruleId, rule.getRuleId());
        Assertions.assertEquals("legacy-host", rule.getHostId());
        Assertions.assertEquals("2.0.1", rule.getRuleVersion());
        Assertions.assertEquals("light-4j", rule.getRuleOwner());
        Assertions.assertEquals("name-matches", rule.getConditionExpression());

        RuleCondition condition = rule.getConditions().iterator().next();
        Assertions.assertEquals("name", condition.getPropertyPath());
        Assertions.assertEquals("equals", condition.getOperatorCode());
        RuleConditionValue conditionValue = condition.getConditionValues().iterator().next();
        Assertions.assertEquals("legacy-value", conditionValue.getConditionValueId());
        Assertions.assertEquals("STRING", conditionValue.getValueTypeCode());
        Assertions.assertEquals("CASE_INSENSITIVE", conditionValue.getRegexFlags());
        Assertions.assertEquals("yyyy-MM-dd", conditionValue.getDateFormat());

        RuleAction action = rule.getActions().iterator().next();
        Assertions.assertEquals(LegacyTestAction.class.getName(), action.getActionClassName());
        Assertions.assertEquals(Boolean.TRUE, action.isConditionResult());
        Assertions.assertEquals("legacy-ruleBodies", action.getParameters().get("source"));
        RuleActionValue actionValue = action.getActionValues().iterator().next();
        Assertions.assertEquals("legacy-action-value", actionValue.getActionValueId());
        Assertions.assertEquals("${name}", actionValue.getValue());
    }

    private static void restoreRuleConfig(Config config, Map<String, Object> originalRuleConfig) throws Exception {
        if (originalRuleConfig != null) {
            config.putInConfigCache(RuleConfig.CONFIG_NAME, originalRuleConfig);
        } else {
            config.clearConfigCache(RuleConfig.CONFIG_NAME);
        }
        setRuleConfigInstance(null);
    }
}
