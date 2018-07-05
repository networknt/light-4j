package com.networknt.service;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Follows given-when-then format.
 * @author Nicholas Azar
 */
public class TestServiceUtil {

    @Test
    public void stringClassWithDefault_constructed_returnsInstance() throws Exception {
        Object object = ServiceUtil.construct("com.networknt.service.AImpl");
        Assert.assertNotNull(object);
    }

    @Test(expected = Exception.class)
    public void stringClassWithoutDefault_constructed_raisesException() throws Exception {
        ServiceUtil.construct("com.networknt.service.ClassWithoutDefaultConstructor");
    }

    @Test
    public void classWithNamedParams_constructed_setsParams() throws Exception {
        Map<String, Object> params = new HashMap<>();
        String testName = "Nick";
        params.put("name", testName);
        Map<String, Object> constructMap = new HashMap<>();
        constructMap.put("com.networknt.service.GImpl", params);
        GImpl object = (GImpl)ServiceUtil.construct(constructMap);
        Assert.assertEquals(testName, object.getName());
    }

    @Test
    public void classWithoutGivenParam_constructed_ignoresParam() throws Exception {
        Map<String, Object> params = new HashMap<>();
        params.put("hello", "howdy");
        params.put("name", "nick");
        Map<String, Object> constructMap = new HashMap<>();
        constructMap.put("com.networknt.service.GImpl", params);
        Assert.assertNotNull(ServiceUtil.construct(constructMap));
    }

    @Test
    public void classWithListedParams_constructed_setsParams() throws Exception {
        Map<String, List<Map<String, Object>>> constructMap = new HashMap<>();
        List<Map<String, Object>> params = new ArrayList<>();
        params.add(Collections.singletonMap("java.lang.String", "Hello"));
        params.add(Collections.singletonMap("java.lang.String", "There"));
        constructMap.put("com.networknt.service.ClassWithoutDefaultConstructor", params);
        ClassWithoutDefaultConstructor result = (ClassWithoutDefaultConstructor)ServiceUtil.construct(constructMap);
        Assert.assertEquals("Hello", result.getFirst());
        Assert.assertEquals("There", result.getSecond());
    }

    @Test(expected = Exception.class)
    public void classWithoutMatchedConstructor_constructed_failsWhenNoDefault() throws Exception {
        Map<String, List<Map<String, Object>>> constructMap = new HashMap<>();
        List<Map<String, Object>> params = new ArrayList<>();
        params.add(Collections.singletonMap("java.lang.String", "Hello"));
        constructMap.put("com.networknt.service.ClassWithoutDefaultConstructor", params);
        ServiceUtil.construct(constructMap);
    }

    @Test
    public void classWithoutMatchedConstructor_constructed_succeedsWhenDefault() throws Exception {
        Map<String, List<Map<String, Object>>> constructMap = new HashMap<>();
        List<Map<String, Object>> params = new ArrayList<>();
        params.add(Collections.singletonMap("java.lang.String", "Hello"));
        constructMap.put("com.networknt.service.GImpl", params);
        Assert.assertNotNull(ServiceUtil.construct(constructMap));
    }
}
