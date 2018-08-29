package com.networknt.whitelist;

import com.networknt.config.Config;
import org.junit.Test;

public class WhitelistConfigTest {
    @Test
    public void testLoadConfig() {
        WhitelistConfig config = (WhitelistConfig) Config.getInstance().getJsonObjectConfig("whitelist", WhitelistConfig.class);
        System.out.println(config);
    }
}
