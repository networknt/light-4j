package com.networknt.common;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

public class DecryptUtilTest {
    @Test
    public void testDecryptMap() {
        Map<String, Object> secretMap = Config.getInstance().getJsonMapConfig("secret");
        DecryptUtil.decryptMap(secretMap);
        Assert.assertEquals("password", secretMap.get("serverKeystorePass"));
    }
}
