package com.networknt.client;

import com.networknt.config.Config;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;


public class ClientConfigValueTest {
    public static final String CONFIG_NAME = "client";
    @Test
    public void testLoadConfig() {
        ClientConfig config = ClientConfig.get(CONFIG_NAME);
        assertEquals(config.getConnectionExpireTime(), 1800000);
    }


}
