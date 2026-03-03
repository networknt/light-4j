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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * Created by Nicholas Azar (@NicholasAzar)
 */
public class TestCentralizedManagement {

    @Test
    public void testMap_mergeApplied_mutatesInPlaceCorrectly() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.string}");
        CentralizedManagement.mergeMap(true, testMap);
        Assertions.assertEquals("test", testMap.get("key").toString());
    }

    @Test
    public void testMap_mergeWhenFieldNotInValues_throwsException() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.somethingNotInValues}");
        try {
            CentralizedManagement.mergeMap(true, testMap);
            Assertions.fail();
        } catch (ConfigException expected) {
            // pass
        }
    }

    @Test
    public void testMap_valueCastToInt() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.int: 1}");
        CentralizedManagement.mergeMap(true, testMap);
        Assertions.assertTrue(testMap.get("key") instanceof Integer);
    }

    @Test
    public void testMap_valueCastToDouble() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.double: 1.1}");
        CentralizedManagement.mergeMap(true, testMap);
        Assertions.assertTrue(testMap.get("key") instanceof Double);
    }

    @Test
    public void testMap_valueCastToBoolean() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.boolean: true}");
        CentralizedManagement.mergeMap(true, testMap);
        Assertions.assertTrue(testMap.get("key") instanceof Boolean);
    }

    @Test
    public void testMap_allowNullOverwrite() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.null: value}");
        CentralizedManagement.mergeMap(true, testMap);
        Assertions.assertNull(testMap.get("key"));
    }

    @Test
    public void testMap_allowEmptyStringOverwrite() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.emptyString: value}");
        CentralizedManagement.mergeMap(true, testMap);
        Assertions.assertEquals("", testMap.get("key"));
    }

    @Test
    // happy pass
    public void testMergeKey() {
        Map<String, Object> map = Config.getInstance().getJsonMapConfig("testKeyInject");
        Map<String, Object> mapAfterInjection = Config.getInstance().getJsonMapConfig("testKeyInject_expect");
        Assertions.assertEquals(mapAfterInjection, map);
    }

    @Test
    public void testMap_key_notAllowEmptyStringOverwrite() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Map<String, Object> testMap = new HashMap<>();
            testMap.put("${TEST.emptyString: key}", "value");
            CentralizedManagement.mergeMap(true, testMap);
        });
    }

    @Test
    public void testMap_key_notAllowNullOverwrite() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Map<String, Object> testMap = new HashMap<>();
            testMap.put("${TEST.null: key}", "value");
            CentralizedManagement.mergeMap(true, testMap);
        });
    }

    @Test
    public void testMap_key_mergeApplied_mutatesInPlaceCorrectly() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("${TEST.string}", "value");
        CentralizedManagement.mergeMap(true, testMap);
        Assertions.assertEquals(testMap.get("test").toString(), "value");
    }

    @Test
    public void testMap_key_mergeWhenFieldNotInValues_throwsException() {
        Assertions.assertThrows(ConfigException.class, () -> {
            Map<String, Object> testMap = new HashMap<>();
            testMap.put("${TEST.somethingNotInValues}", "value");
            CentralizedManagement.mergeMap(true, testMap);
        });
    }

    @Test
    public void testMap_key_keyMustBeString() {
        Assertions.assertThrows(RuntimeException.class, () -> {
            Map<String, Object> testMap = new HashMap<>();
            testMap.put("${TEST.double: 1.1}", "value");
            CentralizedManagement.mergeMap(true, testMap);
        });
    }
}
