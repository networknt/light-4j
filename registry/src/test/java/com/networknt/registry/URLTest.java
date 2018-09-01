/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.networknt.registry;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.networknt.utility.Constants;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * 
 * URL test
 *
 * @author fishermen, stevehu
 * @version V1.0 created at: 2013-7-19
 */

public class URLTest {
    //@Test
    public void testCheckGetMethod() {
        Method[] methods = URL.class.getDeclaredMethods();
        for (Method m : methods) {
            // make sure that get method return an object to prevent accidentally modifications.
            if (m.getName().startsWith("get") && m.getParameterTypes().length > 0) {
                if (m.getReturnType().isPrimitive()) {
                    fail(String.format("URL.%s should not return primitive type", m.getName()));
                }
            }
        }
    }

    @Test
    public void testURL() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("key1", "value1");
        parameters.put("key2", "true");
        parameters.put("key3", "10");
        parameters.put("key4", "3.14");
        parameters.put("key5", "10L");
        parameters.put(URLParamType.nodeType.getName(), Constants.NODE_TYPE_SERVICE);
        parameters.put(URLParamType.version.getName(), URLParamType.version.getValue());

        URL url = new URLImpl("http", "localhost", 8080, "config", parameters);

        String p1 = url.getParameter("key1");
        Assert.assertEquals("value1", p1);
        String p2 = url.getParameter("key6", "default");
        Assert.assertEquals("default", p2);

        Boolean b1 = url.getBooleanParameter("key2", true);
        Assert.assertTrue(b1);

        Boolean b2 = url.getBooleanParameter("key7", true);
        Assert.assertTrue(b2);

        Integer i1 = url.getIntParameter("key3", 0);
        Assert.assertEquals(10, i1.intValue());

        Integer i2 = url.getIntParameter("key8", 9);
        Assert.assertEquals(9, i2.intValue());

        //Float f1 = url.getFloatParameter("key4", 0.0f);
        //Assert.assertEquals(Float.valueOf(3.14f), f1);

        //Float f2 = url.getFloatParameter("key9", 0.01f);
        //Assert.assertEquals(Float.valueOf(0.01f), f2);

        String uri = url.getUri();
        Assert.assertEquals("http://localhost:8080/config", uri);

        String identity = url.getIdentity();
        Assert.assertEquals("http://localhost:8080/default/config/1.0/service", identity);

        URL refUrl = new URLImpl("http", "localhost", 8080, "config");
        boolean canServe = url.canServe(refUrl);
        Assert.assertTrue(canServe);

        String fullStr = url.toFullStr();
        Assert.assertEquals("http://localhost:8080/config?key1=value1&key2=true&key5=10L&key3=10&key4=3.14&nodeType=service&version=1.0&", fullStr);


        URL newUrl = URLImpl.valueOf("http://localhost:8080/config?key1=value1&key2=true&key5=10L&key3=10&key4=3.14&nodeType=service&version=1.0");

        Assert.assertNotNull(newUrl);
    }

    @Test
    public void testDefaultPort() {

    }
}
