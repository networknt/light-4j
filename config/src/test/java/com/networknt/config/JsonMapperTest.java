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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * This test is trying to figure out how long is working in the JsonMapper. It looks
 * like it doesn't work. We need to go back to Avro for all persistence for long values
 * or we have to use string in JSON.
 */
public class JsonMapperTest {
    @Test
    public void testLong() {
        Map<String, Object> map = new HashMap<>();
        map.put("string", "hello");
        map.put("long", 111L);
        map.put("int", 111);
        map.put("Long", Long.valueOf(111));
        map.put("Integer", Integer.valueOf(111));
        String s = JsonMapper.toJson(map);
        System.out.println("s = " + s);
        Map<String, Object> newMap = JsonMapper.string2Map(s);
        System.out.println(newMap);
        //Long l = (Long)newMap.get("Long");
    }

    @Test
    public void testInstant() {
        Map<String, Object> map = new HashMap<>();
        map.put("string", "hello");
        map.put("timestamp", Instant.now());
        String s = JsonMapper.toJson(map);
        System.out.println("s = " + s);
        Map<String, Object> newMap = JsonMapper.string2Map(s);
        System.out.println(newMap);
    }

    @Test
    void testHybridBody() {
        String s = "{\n" +
                "  \"host\": \"lightapi.net\",\n" +
                "  \"service\": \"oauth\",\n" +
                "  \"action\": \"createClient\",\n" +
                "  \"version\": \"0.1.0\",\n" +
                "  \"title\": \"Create Client\",\n" +
                "  \"success\": \"/app/success\",\n" +
                "  \"failure\": \"/app/failure\",\n" +
                "  \"data\": {\n" +
                "    \"hostId\": \"01964b05-552a-7c4b-9184-6857e7f3dc5f\",\n" +
                "    \"authenticateClass\": \"com.networknt.oauth.auth.LightPortalAuth\",\n" +
                "    \"clientName\": \"pylon\",\n" +
                "    \"clientType\": \"trusted\",\n" +
                "    \"clientProfile\": \"service\",\n" +
                "    \"clientScope\": \"portal.r portal.w\",\n" +
                "    \"customClaim\": \"{\\\"roles\\\":\\\"aG9zdC1hZG1pbiB1c2Vy\\\",\\\"userId\\\":\\\"01964b05-5532-7c79-8cde-191dcbd421b8\\\",\\\"host\\\":\\\"01964b05-552a-7c4b-9184-6857e7f3dc5f\\\",\\\"email\\\":\\\"steve.hu@sunlife.com\\\",\\\"eid\\\":\\\"sh35\\\"}\"\n" +
                "  }\n" +
                "}";
        Map<String, Object> map = JsonMapper.string2Map(s);
        Assertions.assertNotNull(map);
        Assertions.assertFalse(map.isEmpty());
    }
}
