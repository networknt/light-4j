package com.networknt.client;

import com.networknt.config.JsonMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ClientConfigValueTest {
    @Test
    public void testLoadConfig() {
        ClientConfig config = ClientConfig.get();
        assertEquals(1800000L, config.getConnectionExpireTime());
    }

    @Test
    public void testTokenConfig() {
        ClientConfig config = ClientConfig.get();
        Map<String, Object> tokenConfig = config.getTokenConfig();
        System.out.println("tokenConfig = " + JsonMapper.toJson(tokenConfig));
    }
}
