/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.junit.Assert;

import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

/**
 * Created by steve on 01/09/16.
 */
public class ConfigDefaultTest extends TestCase {
    Config config = null;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        config = Config.getInstance();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetStringFromFile() throws Exception {
        config.clear();
        String content = config.getStringFromFile("test.json");
        Assert.assertNotNull(content);
    }

    public void testGetJsonMapConfig() throws Exception {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test");
        Assert.assertEquals("default config", configMap.get("value"));
    }

    public void testGetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig)config.getJsonObjectConfig("test", TestConfig.class);
        Assert.assertEquals("default config", tc.getValue());
    }

    public void test1GetJsonMapConfig() throws Exception {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test1");
        Assert.assertEquals("default config", configMap.get("value"));
    }

    public void test1GetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig)config.getJsonObjectConfig("test1", TestConfig.class);
        Assert.assertEquals("default config", tc.getValue());
    }

    public void test2GetJsonMapConfig() throws Exception {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test2");
        Assert.assertEquals("default config", configMap.get("value"));
    }

    public void test2GetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig)config.getJsonObjectConfig("test2", TestConfig.class);
        Assert.assertEquals("default config", tc.getValue());
    }

    public void testGetInputStream() throws Exception {
        try (InputStream is = config.getInputStreamFromFile("test.json")) {
            Assert.assertNotNull(is);
        }
    }

    public void testObjectMapper() throws Exception {
        ObjectMapper mapper = Config.getInstance().getMapper();
        DateModel dm = mapper.readValue("{\"time\" : \"2014-07-02T04:00:00.000000Z\"}", DateModel.class);
        System.out.println(dm.getTime());
        ZoneId zoneId = ZoneId.of("UTC");
        ZonedDateTime zonedDateTime2 =
                ZonedDateTime.of(2014, 7, 2, 4, 0, 0, 0, zoneId);
        System.out.println(zonedDateTime2);
        Assert.assertTrue(zonedDateTime2.equals(dm.getTime()));
    }
}
