package com.networknt.limit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class LimitTemplateConfigTest {
    private static LimitConfig limitConfig;

    @Before
    public void setUp() {
        limitConfig = LimitConfig.load("limit-template");
    }

    @Test
    public void testConfigData() {
        Assert.assertTrue(limitConfig.isEnabled());
        List<LimitQuota> quotas = limitConfig.getRateLimit();
        LimitQuota quota = quotas.get(0);
        Assert.assertEquals(quota.getValue(), 10);
    }

}
