package com.networknt.jaeger.tracing;

import com.networknt.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigReloadIdentityTest {
    @Test
    void reusesDefaultConfigUntilCacheIsCleared() {
        JaegerConfig first = JaegerConfig.load();
        assertSame(first, JaegerConfig.load());

        Config.getInstance().clearConfigCache(JaegerConfig.CONFIG_NAME);
        JaegerConfig reloaded = JaegerConfig.load();

        assertNotSame(first, reloaded);
        assertSame(reloaded, JaegerConfig.load());
    }
}
