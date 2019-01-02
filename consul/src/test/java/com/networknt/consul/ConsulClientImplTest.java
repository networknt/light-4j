package com.networknt.consul;

import com.networknt.config.Config;
import com.networknt.consul.client.ConsulClientImpl;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ConsulClientImplTest {

    private static final ConsulConfig config = (ConsulConfig) Config.getInstance().getJsonObjectConfig(ConsulConstants.CONFIG_NAME, ConsulConfig.class);

   @Test
    public void testWaitProperty() {
        assertEquals("600s", config.getWait());

    }
}
