package com.networknt.basicauth;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class JsonUserConfigTest {
    static final Logger logger = LoggerFactory.getLogger(JsonUserConfigTest.class);
    static final BasicAuthConfig config = BasicAuthConfig.load("basic-auth-json");

    @Test
    public void testDecryption() {
        logger.debug("password for user2 = " + config.getUsers().get("user2").getPassword());
        Assert.assertEquals("password", config.getUsers().get("user2").getPassword());
    }

    @Test
    public void testPaths() {
        Map<String, UserAuth> users = config.getUsers();
        UserAuth user = users.get("user1");
        Assert.assertEquals("user1", user.getUsername());
        Assert.assertEquals("user1pass", user.getPassword());
        Assert.assertEquals(1, user.getPaths().size());
        Assert.assertEquals("/v1/address", user.getPaths().get(0));
    }

}
