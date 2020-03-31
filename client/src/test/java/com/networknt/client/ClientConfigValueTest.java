package com.networknt.client;

import com.networknt.config.Config;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;


public class ClientConfigValueTest {

    private Config config;
    @Before
    public void setUp() {
        config = Config.getInstance();

    }

    @Test
    public void testLoadConfig() {
        assertEquals(ClientConfig.get().getConnectionExpireTime(), 2000000);

    }


}
