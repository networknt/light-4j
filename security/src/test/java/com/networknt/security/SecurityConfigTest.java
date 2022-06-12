package com.networknt.security;

import org.junit.Assert;
import org.junit.Test;

public class SecurityConfigTest {
    @Test
    public void testLoadDefaultConfig() {
        SecurityConfig config = SecurityConfig.load("security");
        Assert.assertTrue(config.isEnableVerifyJwt());
        Assert.assertEquals(2, config.getCertificate().size());
    }

    @Test
    public void testLoadTemplateConfig() {
        SecurityConfig config = SecurityConfig.load("security-template");
        Assert.assertTrue(config.isEnableVerifyJwt());
        Assert.assertEquals(2, config.getCertificate().size());
    }

}
