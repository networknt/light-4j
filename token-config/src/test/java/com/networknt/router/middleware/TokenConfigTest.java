package com.networknt.router.middleware;

import org.junit.Assert;
import org.junit.Test;

public class TokenConfigTest {
    @Test
    public void testLoadConfig() {
        TokenConfig config = TokenConfig.load();
        Assert.assertFalse(config.isEnabled());
    }
}
