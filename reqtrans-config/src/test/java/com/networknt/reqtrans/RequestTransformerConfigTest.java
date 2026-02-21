package com.networknt.reqtrans;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RequestTransformerConfigTest {
    @Test
    public void testConfigLoad() {
        RequestTransformerConfig config = RequestTransformerConfig.load();
        Assertions.assertTrue(config.getMappedConfig().size() > 0);
        Assertions.assertEquals(config.getDefaultBodyEncoding(), "UTF-8");
    }
}
