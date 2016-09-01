package com.networknt.config;

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigPropertyPathTest extends TestCase {
    String homeDir = System.getProperty("user.home");

    public void setUp() throws Exception {
        super.setUp();
        // Add a system property here.
        System.setProperty("undertow-server-config-dir", homeDir);

        Config config = Config.getInstance();

        // write a config file into the user home directory.
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("key", "externalized config");
        config.getMapper().writeValue(new File(homeDir + "/test.json"), map);
    }

    public void tearDown() throws Exception {
        // Remove the test.json from home directory
        File test = new File(homeDir + "/test.json");
        test.delete();
    }

    @Test
    public void testGetConfigFromPropertyPath() throws Exception {
        Config config  = Config.getInstance();
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test");
        Assert.assertEquals("externalized config", configMap.get("key"));
    }

}
