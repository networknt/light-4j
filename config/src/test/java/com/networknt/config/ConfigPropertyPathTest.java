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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.junit.Assert;

public class ConfigPropertyPathTest extends TestCase {

    private static Config config = null;

    private static final String homeDir = System.getProperty("user.home");
    
    @Override
    public void setUp() throws Exception {
        super.setUp();

        // the instance would already be created by other classes since the config is singleton, so need to using
        // reflection to inject field.
        config = Config.getInstance();
        setExternalizedConfigDir(homeDir);

        // write config files
        writeConfigFile("value", "default dir", homeDir);
        writeConfigFile("value", "externalized dir1", homeDir + "/dir1");
        writeConfigFile("value", "externalized dir2", homeDir + "/dir2");
    }

    @Override
    public void tearDown() throws Exception {
        File test1 = new File(homeDir + "/test.json");
        File test2 = new File(homeDir + "/dir1/test.json");
        File test3 = new File(homeDir + "/dir2/test.json");
        File testFolder1 = new File(homeDir + "/dir1");
        File testFolder2 = new File(homeDir + "/dir2");
        test1.delete();
        test2.delete();
        test3.delete();
        testFolder1.delete();
        testFolder2.delete();
        
        setExternalizedConfigDir("");
    }

    // test getting config from light-4j-config-dir
    public void testGetConfig() throws Exception {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test");
        Assert.assertEquals("default dir", configMap.get("value"));
    }

    // test getting config from absolute path "/homeDir/src"
    public void testGetConfigFromAbsPath() throws Exception {
    	setExternalizedConfigDir("");
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test", homeDir + "/dir1");
        Assert.assertEquals("externalized dir1", configMap.get("value"));
    }

    // test getting config from relative path "src"
    public void testGetConfigFromRelPath() {
        config.clear();
        Map<String, Object> configMap = config.getJsonMapConfig("test", "dir1");
        Assert.assertEquals("externalized dir1", configMap.get("value"));
    }

    // test getting config from absolute path "/homeDir/src"
    public void testGetObjectConfigFromAbsPath() throws Exception {
    	setExternalizedConfigDir("");
        config.clear();
        TestConfig configObject = (TestConfig) config.getJsonObjectConfig("test", TestConfig.class, homeDir + "/dir1");
        Assert.assertEquals("externalized dir1", configObject.getValue());
    }

    // test getting config from relative path "src"
    public void testGetObjectConfigFromRelPath() {
        config.clear();
        TestConfig configObject = (TestConfig) config.getJsonObjectConfig("test", TestConfig.class, "dir1");
        Assert.assertEquals("externalized dir1", configObject.getValue());
    }

    // test getting config when the config dir is a list
    public void testGetMapConfigFromMultiPath() throws Exception {
        config.clear();
        setExternalizedConfigDir(homeDir + File.pathSeparator + homeDir + "/dir1"+ File.pathSeparator + homeDir + "/dir2");
        Map<String, Object> configMap = config.getJsonMapConfig("test");
        Assert.assertEquals("externalized dir2", configMap.get("value"));
    }

    private void setExternalizedConfigDir(String externalizedDir) throws Exception {
        Field f1 = config.getClass().getDeclaredField("EXTERNALIZED_PROPERTY_DIR");
        f1.setAccessible(true);
        f1.set(config, externalizedDir.split(File.pathSeparator));
    }

    private void writeConfigFile(String key, String value, String path) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        if (path.equals("")) {
            config.getMapper().writeValue(new File(path), map);
        } else {
            new File(path).mkdirs();
            config.getMapper().writeValue(new File(path + "/test.json"), map);
        }
    }
}
