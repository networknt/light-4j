package com.networknt.config;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;
import org.yaml.snakeyaml.resolver.Resolver;

import com.networknt.config.yml.DecryptConstructor;
import com.networknt.config.yml.YmlConstants;


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
    
    @Test
    public void testDecryptorClass() {
        final Resolver resolver = new Resolver();
        resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN, YmlConstants.CRYPT_FIRST);
        Yaml yaml = new Yaml(new DecryptConstructor("com.networknt.config.TestDecryptor"), new Representer(), new DumperOptions(), resolver);
    	
        Map<String, Object> secret=yaml.load(Config.getInstance().getInputStreamFromFile("secret-map-test2.yml"));
        
        Assert.assertEquals(SECRET+"-test", secret.get("serverKeystorePass"));
    }

    @Test
    public void testAutoDecryptorClass() throws IOException {
        if (System.getenv("config_password") == null) return;
        final Resolver resolver = new Resolver();
        resolver.addImplicitResolver(YmlConstants.CRYPT_TAG, YmlConstants.CRYPT_PATTERN, YmlConstants.CRYPT_FIRST);
        Yaml yaml = new Yaml(new DecryptConstructor("com.networknt.config.TestAutoDecryptor"), new Representer(), new DumperOptions(), resolver);

        Map<String, Object> secret=yaml.load(Config.getInstance().getInputStreamFromFile("secret-map-test2.yml"));

        Assert.assertEquals(SECRET+"-test", secret.get("serverKeystorePass"));
    }
}
