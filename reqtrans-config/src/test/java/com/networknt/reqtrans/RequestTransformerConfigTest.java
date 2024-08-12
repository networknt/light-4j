package com.networknt.reqtrans;

import org.junit.Assert;
import org.junit.Test;

public class RequestTransformerConfigTest {
    @Test
    public void testConfigLoad() {
        RequestTransformerConfig config = RequestTransformerConfig.load();
        Assert.assertTrue(config.getMappedConfig().size() > 0);
        Assert.assertEquals(config.getDefaultBodyEncoding(), "UTF-8");
    }
}
