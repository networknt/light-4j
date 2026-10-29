package com.networknt.basicauth;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.util.SetSystemProperty;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@SetSystemProperty(key = "light_4j_config_password", value = "light")
public class JsonUserConfigTest {
    static final Logger logger = LoggerFactory.getLogger(JsonUserConfigTest.class);
    BasicAuthConfig config;
    @BeforeEach
    void setup() {
        config = BasicAuthConfig.load("basic-auth-json");
    }

    @Test
    public void testDecryption() {
        logger.debug("password for user2 = " + config.getUsers().get("user2").getPassword());
        Assertions.assertEquals("password", config.getUsers().get("user2").getPassword());
    }

    @Test
    public void testPaths() {
        Map<String, UserAuth> users = config.getUsers();
        UserAuth user = users.get("user1");
        Assertions.assertEquals("user1", user.getUsername());
        Assertions.assertEquals("user1pass", user.getPassword());
        Assertions.assertEquals(1, user.getPaths().size());
        Assertions.assertEquals("/v1/address", user.getPaths().get(0));
    }

}
