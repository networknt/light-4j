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

import junit.framework.TestCase;
import org.junit.Assert;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigPropertyPathTest extends TestCase {

    private Config config = null;

    final String homeDir = System.getProperty("user.home");

    @Override
    public void setUp() throws Exception {
        super.setUp();

        config = Config.getInstance();

        // write a config file
        Map<String, Object> map = new HashMap<>();
        map.put("value", "default config");
        config.getMapper().writeValue(new File(homeDir + "/test.json"), map);
        // write another config file with the same name but in different path
        Map<String, Object> map2 = new HashMap<>();
        map2.put("value", "another config");
        new File(homeDir + "/src").mkdirs();
        config.getMapper().writeValue(new File(homeDir + "/src/test.json"), map2);
    }

    @Override
    public void tearDown() throws Exception {
        File test1 = new File(homeDir + "/test.json");
        File test2 = new File(homeDir + "/src");
        test1.delete();
        test2.delete();
    }

    public void testGetConfig() throws Exception {
        System.setProperty("light-4j-config-dir", homeDir);
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test");
        Assert.assertEquals("default config", configMap.get("value"));
    }

    public void testGetConfigFromRelPath() {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test", "src");
        Assert.assertEquals("another config", configMap.get("value"));
    }

    public void testGetObjectConfigFromRelPath() {
        config.clear();
        TestConfig configObject = (TestConfig) config.getJsonObjectConfig("test", TestConfig.class, "src");
        Assert.assertEquals("another config", configObject.getValue());
    }
}
