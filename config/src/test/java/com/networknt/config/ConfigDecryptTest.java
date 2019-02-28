package com.networknt.config;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;


public class ConfigDecryptTest {
	private static final String SECRET="password";
	
    @Test
    public void testDecryptMap() {
        Map<String, Object> secretMap = Config.getInstance().getJsonMapConfigNoCache("secret-map-test");
        Assert.assertEquals(SECRET, secretMap.get("serverKeystorePass"));
        
        secretMap = Config.getInstance().getJsonMapConfig("secret-map-test");
        Assert.assertEquals(SECRET, secretMap.get("serverKeystorePass"));
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
    }
}
