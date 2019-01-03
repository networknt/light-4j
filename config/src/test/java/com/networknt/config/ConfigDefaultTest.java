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

import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;
import org.junit.Assert;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        // case1: regular config
        Assert.assertEquals("default config", configMap.get("value"));
        // case2: config with environment variable
        Assert.assertEquals(System.getenv("HOME"), configMap.get("value1"));
        // case3: config with environment variable, default value exist but not apply
        Assert.assertEquals(System.getenv("LOGNAME"), configMap.get("value2"));
        // case4: config with default value when environment variable is not exist
        Assert.assertEquals("default", configMap.get("value3"));
        // case5: escape from injecting environment variable
        Assert.assertEquals("${ESCAPE}", configMap.get("value4"));
        // case6: override values with centralized file
        Assert.assertEquals("default", configMap.get("value5"));
    }

    public void testGetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig) config.getJsonObjectConfig("test", TestConfig.class);
        Assert.assertEquals("default config", tc.getValue());
        Assert.assertEquals(System.getenv("HOME"), tc.getValue1());
        Assert.assertEquals(System.getenv("LOGNAME"), tc.getValue2());
        Assert.assertEquals("default", tc.getValue3());
        Assert.assertEquals("${ESCAPE}", tc.getValue4());
        Assert.assertEquals("default", tc.getValue5());
    }

    public void test1GetJsonMapConfig() throws Exception {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test1");
        Assert.assertEquals("default config", configMap.get("value"));
        Assert.assertEquals(System.getenv("HOME"), configMap.get("value1"));
        Assert.assertEquals(System.getenv("LOGNAME"), configMap.get("value2"));
        Assert.assertEquals("default", configMap.get("value3"));
        Assert.assertEquals("${ESCAPE}", configMap.get("value4"));
        Assert.assertEquals("default", configMap.get("value5"));
    }

    public void test1GetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig) config.getJsonObjectConfig("test1", TestConfig.class);
        Assert.assertEquals("default config", tc.getValue());
        Assert.assertEquals(System.getenv("HOME"), tc.getValue1());
        Assert.assertEquals(System.getenv("LOGNAME"), tc.getValue2());
        Assert.assertEquals("default", tc.getValue3());
        Assert.assertEquals("${ESCAPE}", tc.getValue4());
        Assert.assertEquals("default", tc.getValue5());
    }

    public void test2GetJsonMapConfig() throws Exception {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test2");
        Assert.assertEquals("default config", configMap.get("value"));
        Assert.assertEquals(System.getenv("HOME"), configMap.get("value1"));
        Assert.assertEquals(System.getenv("LOGNAME"), configMap.get("value2"));
        Assert.assertEquals("default", configMap.get("value3"));
        Assert.assertEquals("${ESCAPE}", configMap.get("value4"));
        Assert.assertEquals("default", configMap.get("value5"));
    }

    public void test2GetJsonObjectConfig() throws Exception {
        config.clear();
        TestConfig tc = (TestConfig) config.getJsonObjectConfig("test2", TestConfig.class);
        Assert.assertEquals("default config", tc.getValue());
        Assert.assertEquals(System.getenv("HOME"), tc.getValue1());
        Assert.assertEquals(System.getenv("LOGNAME"), tc.getValue2());
        Assert.assertEquals("default", tc.getValue3());
        Assert.assertEquals("${ESCAPE}", tc.getValue4());
        Assert.assertEquals("default", tc.getValue5());
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
