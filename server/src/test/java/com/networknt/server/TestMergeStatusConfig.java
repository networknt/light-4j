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
package com.networknt.server;

import com.networknt.config.Config;
import com.networknt.status.Status;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class TestMergeStatusConfig {

    private Config config = null;

    final String homeDir = System.getProperty("user.home");

    @BeforeAll
    public void setUp() throws Exception {
        config = Config.getInstance();
        setExternalizedConfigDir(homeDir);

        Map<String, Object> contents = new HashMap<>();
        contents.put("statusCode", 404);
        contents.put("code", "ERR99999");
        contents.put("message", "test");
        contents.put("description", "test");
        // write config file into externalized path
        writeConfigFile("ERR99999", contents);
    }

    @AfterAll
    public void tearDown() throws Exception {
        File appStatus = new File(homeDir + "/app-status.yml");
        if (appStatus.exists()) {
            appStatus.delete();
        }
    }

    @Test
    public void test1AppStatus() {
        config.clear();
        // test default element without merging with app-status
        Status status0 = new Status("ERR10053", "url");
        Assertions.assertEquals(401, status0.getStatusCode());
        Server.mergeStatusConfig();
        Status status = new Status("ERR99999");
        System.out.println("****************************************************************" + status.toString());
        Assertions.assertEquals(404, status.getStatusCode());
        // test default element after merging
        Status status1 = new Status("ERR10053", "url");
        Assertions.assertEquals(401, status1.getStatusCode());
    }

    @Test
    public void test2DuplicateStatus() {
        config.clear();
        try {
            Server.mergeStatusConfig();
            // second try to make sure duplication status appear
            Server.mergeStatusConfig();
            fail("Expected RuntimeException was not thrown");
        } catch (RuntimeException expected) {
            // pass
        }
    }

    @Test
    public void test3WithoutAppStatus() {
        config.clear();
        File appStatus = new File(homeDir + "/app-status.yml");
        if (appStatus.exists()) {
            appStatus.delete();
        }
        // test default element without merging with app-status
        Status status0 = new Status("ERR10053", "url");
        Assertions.assertEquals(401, status0.getStatusCode());
        Server.mergeStatusConfig();
        // test default element after merging
        Status status1 = new Status("ERR10053", "url");
        Assertions.assertEquals(401, status1.getStatusCode());
    }

    @Test
    public void test4EmptyAppStatus() throws IOException {
        config.clear();
        File appStatus = new File(homeDir + "/app-status.yml");
        if (appStatus.exists()) {
            appStatus.delete();
        }
        appStatus.createNewFile();
        // test default element without merging with app-status
        Status status0 = new Status("ERR10053", "url");
        Assertions.assertEquals(401, status0.getStatusCode());
        Server.mergeStatusConfig();
        // test default element after merging
        Status status1 = new Status("ERR10053", "url");
        Assertions.assertEquals(401, status1.getStatusCode());
    }

    private void setExternalizedConfigDir(String externalizedDir) throws Exception {
        Field f1 = config.getClass().getSuperclass().getDeclaredField("EXTERNALIZED_PROPERTY_DIR");
        f1.setAccessible(true);
        f1.set(config, externalizedDir.split(":"));
    }

    private void writeConfigFile(String key, Object value) throws IOException {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        config.getMapper().writeValue(new File(homeDir + "/app-status.yml"), map);
    }
}
