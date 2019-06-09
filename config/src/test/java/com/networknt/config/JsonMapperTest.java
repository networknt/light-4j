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

import org.junit.Test;

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

}
