package com.networknt.restrans;

import org.junit.Assert;
import org.junit.Test;

public class ResponseTransformerConfigTest {
    @Test
    public void testConfigLoad() {
        ResponseTransformerConfig config = ResponseTransformerConfig.load();
        Assert.assertTrue(config.getMappedConfig().size() > 0);
    }

}
