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
        map.put("Long", new Long(111));
        map.put("Integer", new Integer(111));
        String s = JsonMapper.toJson(map);
        System.out.println("s = " + s);
        Map<String, Object> newMap = JsonMapper.string2Map(s);
        System.out.println(newMap);
        //Long l = (Long)newMap.get("Long");
    }

}
