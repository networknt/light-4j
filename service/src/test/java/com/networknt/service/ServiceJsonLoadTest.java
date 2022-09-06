package com.networknt.service;

import org.junit.Assert;
import org.junit.Test;

public class ServiceJsonLoadTest {
    @Test
    public void testJsonLoad() {
        ServiceConfig config = ServiceConfig.load("service-json");
        Assert.assertTrue(config.getSingletons().size() > 0);
    }
}
