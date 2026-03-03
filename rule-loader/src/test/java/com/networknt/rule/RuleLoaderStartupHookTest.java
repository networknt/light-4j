package com.networknt.rule;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class RuleLoaderStartupHookTest {
    @Test
    @Disabled
    public void testRuleLoader() {
        RuleLoaderStartupHook ruleLoaderStartupHook = new RuleLoaderStartupHook();
        ruleLoaderStartupHook.onStartup();
    }
}
