package com.networknt.config;

import com.networknt.decrypt.Decryptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigInjectionTest {

    private static final String value = "${valueKey}";
    private static final String configKey = "valueKey";
    private static final String configValue = "password";
    private static final String valueMapKey = "values";
    private static final Map<String, Object> valueMap = Config.getInstance().getDefaultJsonMapConfig(valueMapKey);

    // Preserve and restore maps around setter-based tests so they don't affect other tests
    private Map<String, Object> savedDecryptedValueMap;
    private Map<String, Object> savedUndecryptedValueMap;

    @BeforeEach
    public void saveValueMaps() {
        savedDecryptedValueMap = ConfigInjection.getDecryptedValueMap();
        savedUndecryptedValueMap = ConfigInjection.getUndecryptedValueMap();
    }

    @AfterEach
    public void restoreValueMaps() {
        ConfigInjection.setDecryptedValueMap(savedDecryptedValueMap);
        ConfigInjection.setUndecryptedValueMap(savedUndecryptedValueMap);
    }

    /**
     * This test depends on the cached values.yml file. However, we have changed to loading for values.yml to the no cache
     * method to support config-reload locally. Hence, this test is retired.
     */
    @Test
    @Disabled
    public void testGetInjectValueIssue744() {

        Object oldConfigValue = null;
        try {
            oldConfigValue = ConfigInjection.getInjectValue(value, true);
        } catch (Exception ce) {
            // expected exception since no valuemap defined yet.
            Assertions.assertTrue(ce instanceof ConfigException);
        }
        Assertions.assertNull(oldConfigValue);

        Map<String, Object> newValueMap = new HashMap<>();
        newValueMap.put(configKey, configValue);
        Config.getInstance().putInConfigCache(valueMapKey, newValueMap);

        Object newConfigValue = ConfigInjection.getInjectValue(value, true);

        Assertions.assertNotNull(newConfigValue);
        Assertions.assertEquals(configValue, newConfigValue);
    }

    @Test
    public void testConvertEnvVarsUsingDotInValue() {
        String testInput = ConfigInjection.convertEnvVars("server.environment");
        Assertions.assertEquals("SERVER_ENVIRONMENT", testInput);
    }

    @Test
    public void testConvertEnvVarsUsingDotInValueWithMixCases() {
        String testInput = ConfigInjection.convertEnvVars("serVER.ENVironment");
        Assertions.assertEquals("SERVER_ENVIRONMENT", testInput);
    }

    @Test
    public void testConvertEnvVarsUsingDotInValueWithCamelCasing() {
        String testInput = ConfigInjection.convertEnvVars("server.ENVIRONMENT");
        Assertions.assertEquals("SERVER_ENVIRONMENT", testInput);
    }

    @Test
    public void testConvertEnvVarsKafkaPassword() {
        String testInput = ConfigInjection.convertEnvVars("kafka-consumer.password");
        Assertions.assertEquals("KAFKA_CONSUMER_PASSWORD", testInput);
    }

    @Test
    public void testConvertEnvVarsUsingNullValue() {
        String testInput2 = ConfigInjection.convertEnvVars(null);
        Assertions.assertEquals(null, testInput2);
    }

    @Test
    public void testConvertEnvVarsUsingEmptyString() {
        String testInput3 = ConfigInjection.convertEnvVars("");
        Assertions.assertEquals("", testInput3);
    }

    @Test
    @Disabled
    public void testDecryptEnvValueWithEncryptedValue() {

        Decryptor aesDecryptor = ConfigInjection.getDecryptor();
        Object envValue = ConfigInjection.decryptEnvValue(aesDecryptor, "CRYPT:0754fbc37347c136be7725cbf62b6942:71756e13c2400985d0402ed6f49613d0");
        Assertions.assertEquals("password", envValue);
    }

    @Test
    public void testDecryptEnvValueWithNonEncryptedValue() {

        Decryptor aesDecryptor = ConfigInjection.getDecryptor();
        Object envValue = ConfigInjection.decryptEnvValue(aesDecryptor, "password");
        Assertions.assertEquals("password", envValue);
    }

    @Test
    public void testDecryptEnvValueWithEmptyValue() {

        Decryptor aesDecryptor = ConfigInjection.getDecryptor();
        Object envValue = ConfigInjection.decryptEnvValue(aesDecryptor, "");
        Assertions.assertEquals("", envValue);
    }

    @Test
    public void testDecryptEnvValueWithNullValue() {

        Decryptor aesDecryptor = ConfigInjection.getDecryptor();
        Object envValue = ConfigInjection.decryptEnvValue(aesDecryptor, null);
        Assertions.assertEquals(null, envValue);
    }

    /**
     * This test depends on the values.json in the test config folder, which cannot be loaded when values.yml in the same
     * folder and values.yml in the src config folder. You need to rename both values.yml files in order to load the values.json
     * to perform the following test. This test is to ensure that normal backslash in the stringify json is not escaped.
     */
    @Test
    @Disabled
    public void testStringifiesJson() {
        String value = "users: [{\"username\":\"ML\\PAYOUT\"}]";
        String template = "users: ${basic.users:}";
        Object actual = ConfigInjection.getInjectValue(template, false);
        Assertions.assertEquals(value, actual);
    }

    /**
     * Test that setDecryptedValueMap resolves intra-values references (self-injection):
     * a key whose value contains a ${...} placeholder referencing another key in the same map
     * should have the placeholder replaced after the setter is called.
     */
    @Test
    public void testSetDecryptedValueMap_selfInjection_resolvesIntraValuesReferences() {
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("selfInject.baseUrl", "http://localhost:8080");
        valuesMap.put("selfInject.apiEndpoint", "${selfInject.baseUrl}/api");

        ConfigInjection.setDecryptedValueMap(valuesMap);

        Assertions.assertEquals("http://localhost:8080/api", valuesMap.get("selfInject.apiEndpoint"));
        // The base key itself must remain unchanged
        Assertions.assertEquals("http://localhost:8080", valuesMap.get("selfInject.baseUrl"));
    }

    /**
     * Test that setUndecryptedValueMap resolves intra-values references (self-injection):
     * a key whose value contains a ${...} placeholder referencing another key in the same map
     * should have the placeholder replaced after the setter is called.
     */
    @Test
    public void testSetUndecryptedValueMap_selfInjection_resolvesIntraValuesReferences() {
        Map<String, Object> valuesMap = new HashMap<>();
        valuesMap.put("selfInject.host", "myhost");
        valuesMap.put("selfInject.jdbcUrl", "jdbc:mysql://${selfInject.host}:3306/mydb");

        ConfigInjection.setUndecryptedValueMap(valuesMap);

        Assertions.assertEquals("jdbc:mysql://myhost:3306/mydb", valuesMap.get("selfInject.jdbcUrl"));
        // The base key itself must remain unchanged
        Assertions.assertEquals("myhost", valuesMap.get("selfInject.host"));
    }
}
