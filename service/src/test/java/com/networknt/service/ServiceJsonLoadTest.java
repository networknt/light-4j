package com.networknt.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ServiceJsonLoadTest {
    @Test
    public void testJsonLoad() {
        ServiceConfig config = ServiceConfig.load("service-json");
        Assertions.assertTrue(config.getSingletons().size() > 0);
    }
}
