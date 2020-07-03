package com.networknt.config;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import static com.networknt.config.ConfigInjection.getInjectValue;

public class ConfigEscapeTest {
    @Test
    public void testGetInjectValueWithDollar() {
        String s1 = "${password:abc$defg}";
        Object obj = getInjectValue(s1);
        System.out.println(obj);

    }

    @Test
    public void testGetInjectValueWithBackSlash() {
        String s2 = "${password:abc\\$defg}";
        Object obj = getInjectValue(s2);
        System.out.println(obj);
    }

    @Test
    public void testPasswordConfig() {
        Map<String, Object> passwordMap = Config.getInstance().getJsonMapConfigNoCache("password");
        Assert.assertEquals("abc$defg", passwordMap.get("password"));

    }
}
