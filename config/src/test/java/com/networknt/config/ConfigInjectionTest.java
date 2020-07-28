package com.networknt.config;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ConfigInjectionTest {

    private static final String value = "${valueKey}";
    private static final String configKey = "valueKey";
    private static final String configValue = "password";
    private static final String valueMapKey = "values";
    private static final Map<String, Object> valueMap = Config.getInstance().getDefaultJsonMapConfig(valueMapKey);

    @Test
    public void testGetInjectValueIssue744() {

        Object oldConfigValue = null;
        try {
            oldConfigValue = ConfigInjection.getInjectValue(value);
        } catch (Exception ce) {
            // expected exception since no valuemap defined yet.
            assertTrue(ce instanceof ConfigException);
        }
        assertNull(oldConfigValue);

        Map<String, Object> newValueMap = new HashMap<>();
        newValueMap.put(configKey, configValue);
        Config.getInstance().putInConfigCache(valueMapKey, newValueMap);

        Object newConfigValue = ConfigInjection.getInjectValue(value);

        assertNotNull(newConfigValue);
        assertEquals(configValue, newConfigValue);
    }
}