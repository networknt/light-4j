package com.networknt.decrypt;

import com.networknt.common.SecretConfig;
import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

public class AESDescyptorTest {
    public static final String CONFIG_NAME = "secret";
    @Test
    public void descryptorTest() {
        SecretConfig secretConfig = (SecretConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, SecretConfig.class);
        Assert.assertEquals("password", secretConfig.getServerKeyPass());
    }


}
