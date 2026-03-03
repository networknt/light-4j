package com.networknt.limit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LimitTemplateConfigTest {
    private static LimitConfig limitConfig;

    @BeforeEach
    public void setUp() {
        limitConfig = LimitConfig.load("limit-template");
    }

    @Test
    public void testConfigData() {
        Assertions.assertTrue(limitConfig.isEnabled());
        List<LimitQuota> quotas = limitConfig.getRateLimit();
        LimitQuota quota = quotas.get(0);
        Assertions.assertEquals(quota.getValue(), 10);
    }

}
