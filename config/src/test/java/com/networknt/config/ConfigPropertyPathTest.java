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
        System.setProperty("undertow-server-config-dir", homeDir);

        Config config = Config.getInstance();

        // write a config file
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("value", "default config");
        config.getMapper().writeValue(new File(homeDir + "/test.json"), map);
    }

    public void tearDown() throws Exception {
        File test = new File(homeDir + "/test.json");
        test.delete();
    }

    @Test
    public void testGetConfig() throws Exception {
        Config config  = Config.getInstance();
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test");
        Assert.assertEquals("default config", configMap.get("value"));
    }

}
