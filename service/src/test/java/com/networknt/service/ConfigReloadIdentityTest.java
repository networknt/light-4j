package com.networknt.service;

import com.networknt.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigReloadIdentityTest {
    @Test
    void reusesDefaultConfigUntilCacheIsCleared() {
        ServiceConfig first = ServiceConfig.load();
        assertSame(first, ServiceConfig.load());

        Config.getInstance().clearConfigCache(ServiceConfig.CONFIG_NAME);
        ServiceConfig reloaded = ServiceConfig.load();

        assertNotSame(first, reloaded);
        assertSame(reloaded, ServiceConfig.load());
    }
}
