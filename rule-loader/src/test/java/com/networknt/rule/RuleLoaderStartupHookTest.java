package com.networknt.rule;

import org.junit.Test;

public class RuleLoaderStartupHookTest {
    @Test
    public void testRuleLoader() {
        RuleLoaderStartupHook ruleLoaderStartupHook = new RuleLoaderStartupHook();
        ruleLoaderStartupHook.onStartup();
    }
}
