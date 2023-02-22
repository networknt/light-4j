package com.networknt.security;

import org.junit.Assert;
import org.junit.Test;

import java.security.Security;

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

    @Test
    public void testJsonPassThroughClaims() {
        SecurityConfig config = SecurityConfig.load("security-json-claims");
        Assert.assertEquals(2, config.getPassThroughClaims().size());
    }

    @Test
    public void testYamlPassThroughClaims() {
        SecurityConfig config = SecurityConfig.load("security-yaml-claims");
        Assert.assertEquals(2, config.getPassThroughClaims().size());
    }
}
