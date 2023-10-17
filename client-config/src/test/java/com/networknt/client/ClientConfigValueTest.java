package com.networknt.client;

import com.networknt.config.JsonMapper;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertEquals;


public class ClientConfigValueTest {
    @Test
    public void testLoadConfig() {
        ClientConfig config = ClientConfig.get();
        assertEquals(config.getConnectionExpireTime(), 1800000);
    }

    @Test
    public void testTokenConfig() {
        ClientConfig config = ClientConfig.get();
        Map<String, Object> tokenConfig = config.getTokenConfig();
        System.out.println("tokenConfig = " + JsonMapper.toJson(tokenConfig));
    }
}
