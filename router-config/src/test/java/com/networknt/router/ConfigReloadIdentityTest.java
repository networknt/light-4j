package com.networknt.router;

import com.networknt.config.Config;
import com.networknt.router.middleware.PathPrefixServiceConfig;
import com.networknt.router.middleware.PathServiceConfig;
import com.networknt.router.middleware.ServiceDictConfig;
import com.networknt.router.middleware.TokenConfig;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigReloadIdentityTest {
    @Test
    void reusesRouterConfigsUntilCacheIsCleared() {
        assertReloadIdentity(RouterConfig.CONFIG_NAME, RouterConfig::load);
        assertReloadIdentity(PathPrefixServiceConfig.CONFIG_NAME, PathPrefixServiceConfig::load);
        assertReloadIdentity(PathServiceConfig.CONFIG_NAME, PathServiceConfig::load);
        assertReloadIdentity(ServiceDictConfig.CONFIG_NAME, ServiceDictConfig::load);
        assertReloadIdentity(TokenConfig.CONFIG_NAME, TokenConfig::load);
        assertReloadIdentity(OAuthServerConfig.CONFIG_NAME, OAuthServerConfig::load);
    }

    private static void assertReloadIdentity(String configName, Supplier<?> loader) {
        Object first = loader.get();
        Object second = loader.get();
        assertSame(first, second);
        Config.getInstance().clearConfigCache(configName);
        Object reloaded = loader.get();
        assertNotSame(first, reloaded);
        assertSame(reloaded, loader.get());
    }
}
