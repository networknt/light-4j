package com.networknt.decode;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class RequestDecodeConfigTest {
    @Test
    public void loadConfig() {
        RequestDecodeConfig config =
                (RequestDecodeConfig) Config.getInstance().getJsonObjectConfig(RequestDecodeConfig.CONFIG_NAME, RequestDecodeConfig.class);
        Assert.assertEquals(config.getDecoders().size(), 2);
    }
}
