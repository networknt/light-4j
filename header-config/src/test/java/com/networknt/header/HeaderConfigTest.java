package com.networknt.header;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HeaderConfigTest {
    @Test
    public void testLoadExample() {
        HeaderConfig config = HeaderConfig.load("header-example");
        Assertions.assertTrue(config.isEnabled());
    }

    @Test
    public void testConfigMap() {
        HeaderConfig config = HeaderConfig.load();
        Assertions.assertTrue(config.isEnabled());
    }
}
