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

    @Test
    public void testValuesEscape() {
        Map<String, Object> passwordMap = Config.getInstance().getJsonMapConfigNoCache("password");
        Assert.assertEquals("def$g", passwordMap.get("value"));
    }

    @Test
    public void testSql1() {
        Map<String, Object> passwordMap = Config.getInstance().getJsonMapConfigNoCache("password");
        Assert.assertEquals("SELECT JSON_VALUE(abc, '$.foo.bar') FROM def", passwordMap.get("sql1"));
    }

    @Test
    public void testSql2() {
        Map<String, Object> passwordMap = Config.getInstance().getJsonMapConfigNoCache("password");
        Assert.assertEquals("SELECT JSON_VALUE(abc, '$.foo.bar') FROM def", passwordMap.get("sql2"));
    }

}
