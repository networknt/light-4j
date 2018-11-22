package com.networknt.encode;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class ResponseEncodeConfigTest {
    @Test
    public void loadConfig() {
        ResponseEncodeConfig config =
                (ResponseEncodeConfig) Config.getInstance().getJsonObjectConfig(ResponseEncodeConfig.CONFIG_NAME, ResponseEncodeConfig.class);
        Assert.assertEquals(config.getEncoders().size(), 2);
    }
}
