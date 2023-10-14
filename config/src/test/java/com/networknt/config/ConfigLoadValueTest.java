package com.networknt.config;

import org.junit.Assert;
import org.junit.Test;

/**
 * This is the test class to ensure that the load value methods are correct.
 */
public class ConfigLoadValueTest {
    @Test
    public void testLoadBoolean() {
        Assert.assertTrue(Config.loadBooleanValue("test", "true"));
        Assert.assertTrue(Config.loadBooleanValue("test", "TRUE"));
        Assert.assertTrue(Config.loadBooleanValue("test", true));
        Assert.assertFalse(Config.loadBooleanValue("test", false));
        Assert.assertFalse(Config.loadBooleanValue("test", "false"));
        Assert.assertFalse(Config.loadBooleanValue("test", "abc"));
    }

    @Test(expected = ConfigException.class)
    public void testLoadBooleanException() {
        Config.loadBooleanValue("test", 123);
    }

    @Test
    public void testLoadInteger() {
        Assert.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", 123));
        Assert.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", "123"));
    }

    @Test(expected = ConfigException.class)
    public void testLoadIntegerExceptionBoolean() {
        Assert.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", true));
    }

    @Test(expected = ConfigException.class)
    public void testLoadIntegerExceptionFloat() {
        Assert.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", 123.45));
    }

    @Test(expected = NumberFormatException.class)
    public void testLoadIntegerNumberFormatException() {
        Assert.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", "123.45"));
    }

    @Test
    public void testLoadLong() {
        Assert.assertEquals(Long.valueOf(123), Config.loadLongValue("test", 123L));
        Assert.assertEquals(Long.valueOf(123), Config.loadLongValue("test", 123));
        Assert.assertEquals(Long.valueOf(123), Config.loadLongValue("test", Integer.valueOf(123)));
        Assert.assertEquals(Long.valueOf(123), Config.loadLongValue("test", Long.valueOf(123)));
        Assert.assertEquals(Long.valueOf(123), Config.loadLongValue("test", "123"));
    }

    @Test(expected = ConfigException.class)
    public void testLoadLongExceptionFloat() {
        Assert.assertEquals(Long.valueOf(123), Config.loadLongValue("test", 123.45));
    }

    @Test(expected = NumberFormatException.class)
    public void testLoadLongExceptionStringFloat() {
        Assert.assertEquals(Long.valueOf(123), Config.loadLongValue("test", "123.45"));
    }

    @Test(expected = NumberFormatException.class)
    public void testLoadLongExceptionNotNumber() {
        Assert.assertEquals(Long.valueOf(123), Config.loadLongValue("test", "abc"));
    }

}
