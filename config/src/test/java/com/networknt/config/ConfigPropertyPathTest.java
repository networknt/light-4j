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
import org.junit.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class ConfigPropertyPathTest extends TestCase {
    final String homeDir = System.getProperty("user.home");

    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("light-java-config-dir", homeDir);

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

    public void testGetConfig() throws Exception {
        Config config  = Config.getInstance();
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test");
        Assert.assertEquals("default config", configMap.get("value"));
    }

}
