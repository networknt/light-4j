package com.networknt.restrans;

import com.networknt.config.Config;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigReloadIdentityTest {
    @Test
    void reusesResponseCacheConfigUntilCacheIsCleared() {
        ResponseCacheConfig first = ResponseCacheConfig.load();
        assertSame(first, ResponseCacheConfig.load());

        Config.getInstance().clearConfigCache(ResponseCacheConfig.CONFIG_NAME);
        ResponseCacheConfig reloaded = ResponseCacheConfig.load();

        assertNotSame(first, reloaded);
        assertSame(reloaded, ResponseCacheConfig.load());
    }

    @Test
    void reusesResponseFilterConfigUntilCacheIsCleared() {
        ResponseFilterConfig first = ResponseFilterConfig.load();
        assertSame(first, ResponseFilterConfig.load());

        Config.getInstance().clearConfigCache(ResponseFilterConfig.CONFIG_NAME);
        ResponseFilterConfig reloaded = ResponseFilterConfig.load();

        assertNotSame(first, reloaded);
        assertSame(reloaded, ResponseFilterConfig.load());
    }
}
