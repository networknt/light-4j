/*
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

package io.dropwizard.metrics.influxdb.data;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by steve on 2016-10-16.
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
