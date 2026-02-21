package com.networknt.router.middleware;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TokenConfigTest {
    @Test
    public void testLoadConfig() {
        TokenConfig config = TokenConfig.load();
        Assertions.assertFalse(config.isEnabled());
    }
}
