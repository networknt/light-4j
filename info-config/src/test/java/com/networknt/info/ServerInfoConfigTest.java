package com.networknt.info;

import org.junit.Test;

public class ServerInfoConfigTest {
    @Test
    public void testLoad() {
        ServerInfoConfig serverInfoConfig = ServerInfoConfig.load();
        assert(serverInfoConfig != null);
    }
}
