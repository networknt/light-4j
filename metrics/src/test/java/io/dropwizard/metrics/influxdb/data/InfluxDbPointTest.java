package io.dropwizard.metrics.influxdb.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by stevehu on 2016-10-16.
 */
public class InfluxDbPointTest {
    @Test
    public void testMap2StringEmpty() {
        Map<String, String> map = new HashMap<>();
        Assert.assertEquals("", InfluxDbPoint.map2String(map));
    }
    @Test
    public void testMap2String() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");
        map.put("key3", "value3");
        Assert.assertEquals("key1=value1,key2=value2,key3=value3", InfluxDbPoint.map2String(map));
    }
    @Test
    public void testToString() {
        Map<String, String> tags = new LinkedHashMap<>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        tags.put("key3", "value3");
        InfluxDbPoint point = new InfluxDbPoint("counter", tags, 1234567890, "123");
        Assert.assertEquals("counter,key1=value1,key2=value2,key3=value3 value=123 1234567890", point.toString());
    }
}
