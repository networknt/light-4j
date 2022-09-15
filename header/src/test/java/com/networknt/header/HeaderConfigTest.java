package com.networknt.header;

import org.junit.Assert;
import org.junit.Test;

public class HeaderConfigTest {
    @Test
    public void testLoadExample() {
        HeaderConfig config = HeaderConfig.load("header-example");
        Assert.assertTrue(config.isEnabled());
    }

    @Test
    public void testConfigMap() {
        HeaderConfig config = HeaderConfig.load();
        Assert.assertTrue(config.isEnabled());
    }
}
