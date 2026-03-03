package com.networknt.health;

import org.junit.jupiter.api.Test;

public class HealthConfigTest {
    @Test
    public void testLoad() {
        HealthConfig config = HealthConfig.load();
        assert(config != null);
    }
}
