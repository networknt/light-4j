package com.networknt.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * This is the test class to ensure that the load value methods are correct.
 */
public class ConfigLoadValueTest {
    @Test
    public void testLoadBoolean() {
        Assertions.assertTrue(Config.loadBooleanValue("test", "true"));
        Assertions.assertTrue(Config.loadBooleanValue("test", "TRUE"));
        Assertions.assertTrue(Config.loadBooleanValue("test", true));
        Assertions.assertFalse(Config.loadBooleanValue("test", false));
        Assertions.assertFalse(Config.loadBooleanValue("test", "false"));
        Assertions.assertFalse(Config.loadBooleanValue("test", "abc"));
    }

    @Test
    public void testLoadBooleanException() {
        Assertions.assertThrows(ConfigException.class, () -> {
            Config.loadBooleanValue("test", 123);
        });
    }

    @Test
    public void testLoadInteger() {
        Assertions.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", 123));
        Assertions.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", "123"));
    }

    @Test
    public void testLoadIntegerExceptionBoolean() {
        Assertions.assertThrows(ConfigException.class, () -> {
            Assertions.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", true));
        });
    }

    @Test
    public void testLoadIntegerExceptionFloat() {
        Assertions.assertThrows(ConfigException.class, () -> {
            Assertions.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", 123.45));
        });
    }

    @Test
    public void testLoadIntegerNumberFormatException() {
        Assertions.assertThrows(NumberFormatException.class, () -> {
            Assertions.assertEquals(Integer.valueOf(123), Config.loadIntegerValue("test", "123.45"));
        });
    }

    @Test
    public void testLoadLong() {
        Assertions.assertEquals(Long.valueOf(123), Config.loadLongValue("test", 123L));
        Assertions.assertEquals(Long.valueOf(123), Config.loadLongValue("test", 123));
        Assertions.assertEquals(Long.valueOf(123), Config.loadLongValue("test", Integer.valueOf(123)));
        Assertions.assertEquals(Long.valueOf(123), Config.loadLongValue("test", Long.valueOf(123)));
        Assertions.assertEquals(Long.valueOf(123), Config.loadLongValue("test", "123"));
    }

    @Test
    public void testLoadLongExceptionFloat() {
        Assertions.assertThrows(ConfigException.class, () -> {
            Assertions.assertEquals(Long.valueOf(123), Config.loadLongValue("test", 123.45));
        });
    }

    @Test
    public void testLoadLongExceptionStringFloat() {
        Assertions.assertThrows(NumberFormatException.class, () -> {
            Assertions.assertEquals(Long.valueOf(123), Config.loadLongValue("test", "123.45"));
        });
    }

    @Test
    public void testLoadLongExceptionNotNumber() {
        Assertions.assertThrows(NumberFormatException.class, () -> {
            Assertions.assertEquals(Long.valueOf(123), Config.loadLongValue("test", "abc"));
        });
    }

}
