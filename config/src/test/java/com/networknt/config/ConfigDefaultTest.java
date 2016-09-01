package com.networknt.config;

import com.fasterxml.jackson.databind.JsonNode;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.util.Map;

/**
 * Created by steve on 01/09/16.
 */
public class ConfigDefaultTest extends TestCase {
    Config config = null;
    public void setUp() throws Exception {
        super.setUp();
        config = Config.getInstance();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testGetStringFromFile() throws Exception {
        config.clear();
        String content = config.getStringFromFile("test.json");
        Assert.assertNotNull(content);
    }

    @Test
    public void testGetJsonMapConfig() throws Exception {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test");
        Assert.assertEquals("default config", configMap.get("value"));
    }

    @Test
    public void testGetJsonNodeConfig() throws Exception {
        config.clear();
        JsonNode node = config.getJsonNodeConfig("test");
        Assert.assertEquals("default config", node.get("value").textValue());
    }

    @Test
    public void testGetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig)config.getJsonObjectConfig("test", TestConfig.class);
        Assert.assertEquals("default config", tc.getValue());
    }

    @Test
    public void testGetInputStream() throws Exception {
        InputStream is = config.getInputStreamFromFile("test.json");
        try {
            Assert.assertNotNull(is);
        } finally {
            is.close();
        }
    }


}
