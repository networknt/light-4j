package com.networknt.basic;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicConfigTest {
    static final Logger logger = LoggerFactory.getLogger(BasicConfigTest.class);

    static final String CONFIG_NAME = "basic";
    static final BasicConfig config = (BasicConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, BasicConfig.class);

    @Test
    public void testDecryption() {
        logger.debug("password for user2 = " + config.getUsers().get(1).get("password"));
        Assert.assertEquals("password", config.getUsers().get(1).get("password"));
    }
}
