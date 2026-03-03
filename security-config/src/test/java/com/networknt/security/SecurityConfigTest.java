package com.networknt.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.security.Security;

public class SecurityConfigTest {
    @Test
    public void testLoadDefaultConfig() {
        SecurityConfig config = SecurityConfig.load("security");
        Assertions.assertTrue(config.isEnableVerifyJwt());
        Assertions.assertEquals(2, config.getCertificate().size());
    }

    @Test
    public void testLoadTemplateConfig() {
        SecurityConfig config = SecurityConfig.load("security-template");
        Assertions.assertTrue(config.isEnableVerifyJwt());
        Assertions.assertEquals(2, config.getCertificate().size());
    }

    @Test
    public void testJsonPassThroughClaims() {
        SecurityConfig config = SecurityConfig.load("security-json-claims");
        Assertions.assertEquals(2, config.getPassThroughClaims().size());
    }

    @Test
    public void testYamlPassThroughClaims() {
        SecurityConfig config = SecurityConfig.load("security-yaml-claims");
        Assertions.assertEquals(2, config.getPassThroughClaims().size());
    }
}
