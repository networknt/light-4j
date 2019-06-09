/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
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

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by steve on 01/09/16.
 */
public class ConfigDefaultTest extends TestCase {
    Config config = null;
    Map<String, Object> testMap = null;
    String OS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);


    @Override
    public void setUp() throws Exception {
        super.setUp();
        config = Config.getInstance();
        testMap = new HashMap<>();
        testMap.put("key1", "element1");
        testMap.put("key2", "element2");
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
        // case1: regular config
        Assert.assertEquals("default config", configMap.get("value"));
        // case2: config with environment variable
        if (!OS.startsWith("windows")) {
            Assert.assertEquals(System.getenv("HOME"), configMap.get("value1"));
        }
        // case3: escape from injecting environment variable
        Assert.assertEquals("${ESCAPE}", configMap.get("value2"));
        // case4: override to map with centralized file
        Assert.assertEquals("test", configMap.get("value3"));
        // case5: override to map with centralized file
        Assert.assertEquals(Arrays.asList("element1", "element2"), configMap.get("value4"));
        // case6: override to map with centralized file
        Assert.assertEquals(testMap, configMap.get("value5"));
        // case7: default value start with $ but not escape
        Assert.assertEquals("$abc", configMap.get("value6"));
    }

    public void testGetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig) config.getJsonObjectConfig("test", TestConfig.class);
        // case1: regular config
        Assert.assertEquals("default config", tc.getValue());
        // case2: config with environment variable
        if (!OS.startsWith("windows")) {
            Assert.assertEquals(System.getenv("HOME"), tc.getValue1());
        }
        // case3: escape from injecting environment variable
        Assert.assertEquals("${ESCAPE}", tc.getValue2());
        // case4: override to map with centralized file
        Assert.assertEquals("test", tc.getValue3());
        // case5: override to map with centralized file
        Assert.assertEquals(Arrays.asList("element1", "element2"), tc.getValue4());
        // case6: override to map with centralized file
        Assert.assertEquals(testMap, tc.getValue5());
        // case7: default value start with $ but not escape
        Assert.assertEquals("$abc", tc.getValue6());
    }

    public void test1GetJsonMapConfig() throws Exception {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test1");
        // case1: regular config
        Assert.assertEquals("default config", configMap.get("value"));
        // case2: config with environment variable
        if (!OS.startsWith("windows")) {
            Assert.assertEquals(System.getenv("HOME"), configMap.get("value1"));
        }
        // case3: escape from injecting environment variable
        Assert.assertEquals("${ESCAPE}", configMap.get("value2"));
        // case4: override to map with centralized file
        Assert.assertEquals("test", configMap.get("value3"));
        // case5: override to map with centralized file
        Assert.assertEquals(Arrays.asList("element1", "element2"), configMap.get("value4"));
        // case6: override to map with centralized file
        Assert.assertEquals(testMap, configMap.get("value5"));
        // case7: default value start with $ but not escape
        Assert.assertEquals("$abc", configMap.get("value6"));
    }

    public void test1GetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig) config.getJsonObjectConfig("test1", TestConfig.class);
        // case1: regular config
        Assert.assertEquals("default config", tc.getValue());
        // case2: config with environment variable
        if (!OS.startsWith("windows")) {
            Assert.assertEquals(System.getenv("HOME"), tc.getValue1());
        }
        // case3: escape from injecting environment variable
        Assert.assertEquals("${ESCAPE}", tc.getValue2());
        // case4: override to map with centralized file
        Assert.assertEquals("test", tc.getValue3());
        // case5: override to map with centralized file
        Assert.assertEquals(Arrays.asList("element1", "element2"), tc.getValue4());
        // case6: override to map with centralized file
        Assert.assertEquals(testMap, tc.getValue5());
        // case7: default value start with $ but not escape
        Assert.assertEquals("$abc", tc.getValue6());
    }

    public void test2GetJsonMapConfig() throws Exception {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test2");
        // case1: regular config
        Assert.assertEquals("default config", configMap.get("value"));
        // case2: config with environment variable
        if (!OS.startsWith("windows")) {
            Assert.assertEquals(System.getenv("HOME"), configMap.get("value1"));
        }
        // case3: escape from injecting environment variable
        Assert.assertEquals("${ESCAPE}", configMap.get("value2"));
        // case4: override to map with centralized file
        Assert.assertEquals("test", configMap.get("value3"));
        // case5: override to map with centralized file
        Assert.assertEquals(Arrays.asList("element1", "element2"), configMap.get("value4"));
        // case6: override to map with centralized file
        Assert.assertEquals(testMap, configMap.get("value5"));
        // case7: default value start with $ but not escape
        Assert.assertEquals("$abc", configMap.get("value6"));
    }

    public void test2GetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig) config.getJsonObjectConfig("test2", TestConfig.class);
        // case1: regular config
        Assert.assertEquals("default config", tc.getValue());
        // case2: config with environment variable
        if (!OS.startsWith("windows")) {
            Assert.assertEquals(System.getenv("HOME"), tc.getValue1());
        }
        // case3: escape from injecting environment variable
        Assert.assertEquals("${ESCAPE}", tc.getValue2());
        // case4: override to map with centralized file
        Assert.assertEquals("test", tc.getValue3());
        // case5: override to map with centralized file
        Assert.assertEquals(Arrays.asList("element1", "element2"), tc.getValue4());
        // case6: override to map with centralized file
        Assert.assertEquals(testMap, tc.getValue5());
        // case7: default value start with $ but not escape
        Assert.assertEquals("$abc", tc.getValue6());
    }

    public void testGetNullValueJsonMapConfig() {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test_nullValue");
        Assert.assertEquals(null, configMap.get("value"));
    }

    public void testEmptyStringValueJsonMapConfig() {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test_emptyString");
        Assert.assertEquals("", configMap.get("value"));
    }

    
    public void testInjectionExclusionConfig() {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test_exclusion");
        Assert.assertEquals("${TEST.string}", configMap.get("key"));
    }

    public void testInvalidValueJsonMapConfig() {
        config.clear();
        try {
            config.getJsonMapConfig("test_invalid");
            fail();
        } catch(Exception e) {}
    }

    public void testGetInputStream() throws Exception {
        try (InputStream is = config.getInputStreamFromFile("test.json")) {
            Assert.assertNotNull(is);
        }
    }

    public void testObjectMapperZonedDateTime() throws Exception {
        ObjectMapper mapper = Config.getInstance().getMapper();
        ZonedDateTimeModel dm = mapper.readValue("{\"time\" : \"2014-07-02T04:00:00.000000Z\"}",
                ZonedDateTimeModel.class);
        System.out.println(dm.getTime());
        ZoneId zoneId = ZoneId.of("UTC");
        ZonedDateTime zonedDateTime2 = ZonedDateTime.of(2014, 7, 2, 4, 0, 0, 0, zoneId);
        System.out.println(zonedDateTime2);
        Assert.assertTrue(zonedDateTime2.equals(dm.getTime()));
    }

    public void testObjectMapperLocalDateTime() throws Exception {
        ObjectMapper mapper = Config.getInstance().getMapper();
        LocalDateTimeModel dm = mapper.readValue("{\"time\" : \"1999-01-02T04:05:06.700000Z\"}", LocalDateTimeModel.class);
        System.out.println(dm.getTime());
        LocalDateTime dm2 = LocalDateTime.of(1999, 1, 2, 4, 5, 6, 700000000);
        System.out.println(dm2);
        Assert.assertTrue(dm2.equals(dm.getTime()));
    }
    
    public void testObjectMapperLocalDate() throws Exception {
        ObjectMapper mapper = Config.getInstance().getMapper();
        LocalDateModel dm = mapper.readValue("{\"date\" : \"1999-02-03T04:05:06.700000Z\"}", LocalDateModel.class);
        System.out.println(dm.getDate());
        LocalDate dm2 = LocalDate.of(1999, 2, 3);
        System.out.println(dm2);
        Assert.assertTrue(dm2.equals(dm.getDate()));
    }
}
