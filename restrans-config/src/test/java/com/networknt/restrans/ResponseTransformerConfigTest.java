package com.networknt.restrans;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ResponseTransformerConfigTest {
    @Test
    public void testConfigLoad() {
        ResponseTransformerConfig config = ResponseTransformerConfig.load();
        Assertions.assertTrue(config.getMappedConfig().size() > 0);
        Assertions.assertEquals(config.getDefaultBodyEncoding(), "UTF-8");
    }

}
