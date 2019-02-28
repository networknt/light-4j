package com.networknt.config;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;


public class ConfigDecryptTest {
	private static final String SECRET="password";
	private static final String TEST_MAP="testMap";
	private static final String TEST_ARRAY="testArray";
	
    @SuppressWarnings("unchecked")
	@Test
    public void testDecryptMap() {
        Map<String, Object> secretMap = Config.getInstance().getJsonMapConfigNoCache("secret-map-test");
        Assert.assertEquals(SECRET, secretMap.get("serverKeystorePass"));
        
        secretMap = Config.getInstance().getJsonMapConfig("secret-map-test");
        Assert.assertEquals(SECRET, secretMap.get("serverKeystorePass"));
        
        List<String> testArray = (List<String>) secretMap.get(TEST_ARRAY);
        
        Assert.assertTrue(testArray.size()>0);
        
        for (String s: testArray) {
        	Assert.assertEquals(SECRET, s);
        }
        
        Map<String, String> testMap = (Map<String, String>) secretMap.get(TEST_MAP);
        
        Assert.assertTrue(testMap.size()>0);
        
        for (String s: testMap.values()) {
        	Assert.assertEquals(SECRET, s);
        }
    }
    
    @Test
    public void testDecryptObject() {
    	SecretConfig secretConfig = (SecretConfig) Config.getInstance().getJsonObjectConfig("secret-object-test", SecretConfig.class);
    	
    	Assert.assertEquals(SECRET, secretConfig.getServerKeystorePass());
    	Assert.assertEquals(SECRET, secretConfig.getServerKeyPass());
    	Assert.assertEquals(SECRET, secretConfig.getServerTruststorePass());
    	Assert.assertEquals(SECRET, secretConfig.getClientKeystorePass());
    	Assert.assertEquals(SECRET, secretConfig.getClientKeyPass());
    	Assert.assertEquals(SECRET, secretConfig.getClientTruststorePass());
    	
        Assert.assertTrue(secretConfig.getTestArray().size()>0);
        
        for (String s: secretConfig.getTestArray()) {
        	Assert.assertEquals(SECRET, s);
        }
        
        Assert.assertTrue(secretConfig.getTestMap().size()>0);
        
        for (String s: secretConfig.getTestMap().values()) {
        	Assert.assertEquals(SECRET, s);
        }
    }
}
