package com.networknt.proxy;

import com.networknt.config.Config;
import com.networknt.proxy.mras.MrasConfig;
import com.networknt.proxy.salesforce.SalesforceConfig;
import com.networknt.proxy.tableau.TableauConfig;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ConfigReloadIdentityTest {
    @Test
    void reusesDefaultConfigUntilCacheIsCleared() {
        assertReloadIdentity(ExternalServiceConfig.CONFIG_NAME, ExternalServiceConfig::load);
        assertReloadIdentity(MrasConfig.CONFIG_NAME, MrasConfig::load);
        assertReloadIdentity(SalesforceConfig.CONFIG_NAME, SalesforceConfig::load);
        assertReloadIdentity(TableauConfig.CONFIG_NAME, TableauConfig::load);
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
