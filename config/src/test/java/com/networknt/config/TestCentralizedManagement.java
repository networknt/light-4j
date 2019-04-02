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

import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Nicholas Azar (@NicholasAzar)
 */
public class TestCentralizedManagement extends TestCase {

    @Test
    public void testMap_mergeApplied_mutatesInPlaceCorrectly() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.string}");
        CentralizedManagement.mergeMap(testMap);
        Assert.assertEquals("test", testMap.get("key").toString());
    }

    @Test
    public void testMap_mergeWhenFieldNotInValues_throwsException() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.somethingNotInValues}");
        try {
            CentralizedManagement.mergeMap(testMap);
            fail();
        } catch (ConfigException expected) {
            // pass
        }
    }

    @Test
    public void testMap_valueCastToInt() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.int: 1}");
        CentralizedManagement.mergeMap(testMap);
        Assert.assertTrue(testMap.get("key") instanceof Integer);
    }

    @Test
    public void testMap_valueCastToDouble() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.double: 1.1}");
        CentralizedManagement.mergeMap(testMap);
        Assert.assertTrue(testMap.get("key") instanceof Double);
    }

    @Test
    public void testMap_valueCastToBoolean() {
        Map<String, Object> testMap = new HashMap<>();
        testMap.put("key", "${TEST.boolean: true}");
        CentralizedManagement.mergeMap(testMap);
        Assert.assertTrue(testMap.get("key") instanceof Boolean);
    }
}
