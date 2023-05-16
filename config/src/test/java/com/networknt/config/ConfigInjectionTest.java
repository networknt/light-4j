package com.networknt.config;

import com.networknt.decrypt.Decryptor;
import org.junit.Assert;
import org.junit.Ignore;
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

    /**
     * This test depends on the cached values.yml file. However, we have changed to loading for values.yml to the no cache
     * method to support config-reload locally. Hence, this test is retired.
     */
    @Test
    @Ignore
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

    @Test
    public void testConvertEnvVarsUsingDotInValue() {
        String testInput = ConfigInjection.convertEnvVars("server.environment");
        Assert.assertEquals("SERVER_ENVIRONMENT", testInput);
    }

    @Test
    public void testConvertEnvVarsUsingDotInValueWithMixCases() {
        String testInput = ConfigInjection.convertEnvVars("serVER.ENVironment");
        Assert.assertEquals("SERVER_ENVIRONMENT", testInput);
    }

    @Test
    public void testConvertEnvVarsUsingDotInValueWithCamelCasing() {
        String testInput = ConfigInjection.convertEnvVars("server.ENVIRONMENT");
        Assert.assertEquals("SERVER_ENVIRONMENT", testInput);
    }


    @Test
    public void testConvertEnvVarsUsingNullValue() {
        String testInput2 = ConfigInjection.convertEnvVars(null);
        Assert.assertEquals(null, testInput2);
    }

    @Test
    public void testConvertEnvVarsUsingEmptyString() {
        String testInput3 = ConfigInjection.convertEnvVars("");
        Assert.assertEquals("", testInput3);
    }

    @Test
    public void testDecryptEnvValueWithEncryptedValue() {

        Decryptor aesDecryptor = ConfigInjection.getDecryptor();
        Object envValue = ConfigInjection.decryptEnvValue(aesDecryptor, "CRYPT:0754fbc37347c136be7725cbf62b6942:71756e13c2400985d0402ed6f49613d0");
        Assert.assertEquals("password", envValue);
    }

    @Test
    public void testDecryptEnvValueWithNonEncryptedValue() {

        Decryptor aesDecryptor = ConfigInjection.getDecryptor();
        Object envValue = ConfigInjection.decryptEnvValue(aesDecryptor, "password");
        Assert.assertEquals("password", envValue);
    }

    @Test
    public void testDecryptEnvValueWithEmptyValue() {

        Decryptor aesDecryptor = ConfigInjection.getDecryptor();
        Object envValue = ConfigInjection.decryptEnvValue(aesDecryptor, "");
        Assert.assertEquals("", envValue);
    }

    @Test
    public void testDecryptEnvValueWithNullValue() {

        Decryptor aesDecryptor = ConfigInjection.getDecryptor();
        Object envValue = ConfigInjection.decryptEnvValue(aesDecryptor, null);
        Assert.assertEquals(null, envValue);
    }

}