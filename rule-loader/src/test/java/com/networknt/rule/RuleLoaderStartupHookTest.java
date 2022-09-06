package com.networknt.rule;

import org.junit.Ignore;
import org.junit.Test;

public class RuleLoaderStartupHookTest {
    @Test
    @Ignore
    public void testRuleLoader() {
        RuleLoaderStartupHook ruleLoaderStartupHook = new RuleLoaderStartupHook();
        ruleLoaderStartupHook.onStartup();
    }
}
