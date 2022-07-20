package com.networknt.config;

import org.junit.Assert;
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


    @Test
    public void testGetDecryptorWithValidInput() {

        String testDecryptor = ConfigInjection.getDecryptor().decrypt("CRYPT:EqDVC30YKUDTMLXSIS5OpOqeP+K4w0dPaFfaJPfzIT8=");
        Assert.assertEquals("password", testDecryptor);
    }


    @Test(expected = RuntimeException.class)
    public void testGetDecryptorWithInvalidInput() {

        String testDecryptor1 = ConfigInjection.getDecryptor().decrypt("EqDVC30YKUDTMLXSIS5OpOqeP+K4w0dPaFfaJPfzIT8=");
        Assert.assertEquals(new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'."), testDecryptor1);
    }

    @Test(expected = NullPointerException.class)
    public void testGetDecryptorWithNull() {

        String testDecryptor2 = ConfigInjection.getDecryptor().decrypt(null);
        Assert.assertEquals(new RuntimeException("Unable to retrieve the configuration."), testDecryptor2);
    }

    @Test(expected = RuntimeException.class)
    public void testGetDecryptorWithEmptyInput() {

        String testDecryptor1 = ConfigInjection.getDecryptor().decrypt("");
        Assert.assertEquals(new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'."), testDecryptor1);
    }

    @Test
    public void testConvertEnvVarsUsingDotInValue(){
        String testInput = ConfigInjection.convertEnvVars("server.environment");
        Assert.assertEquals("SERVER_ENVIRONMENT",testInput);
    }


    @Test
    public void testConvertEnvVarsUsingNullValue() {
        String testInput2 = ConfigInjection.convertEnvVars(null);
        Assert.assertEquals(null,testInput2);
    }

    @Test
    public void testConvertEnvVarsUsingEmptyString(){
        String testInput3 = ConfigInjection.convertEnvVars("");
        Assert.assertEquals("",testInput3);
    }


}