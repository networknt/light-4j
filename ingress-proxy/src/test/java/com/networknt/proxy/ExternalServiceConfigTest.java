package com.networknt.proxy;

import com.networknt.info.ServerInfoConfig;
import com.networknt.info.ServerInfoUtil;
import com.networknt.server.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExternalServiceConfigTest {
    @Test
    public void testHostMapping() {
        ExternalServiceConfig config = new ExternalServiceConfig();
        assert config.getPathHostMappings().size() == 4;

        if (config.getPathHostMappings() != null) {
            for (String[] parts : config.getPathHostMappings()) {
                if ("/sharepoint".startsWith(parts[0])) {
                    String endpoint = parts[0] + "@" + "post";
                    assert endpoint.equals("/sharepoint@post");
                }
            }
        }
    }
    @Test
    public void testUrlRewriteRules() {
        ExternalServiceConfig config = new ExternalServiceConfig();
        assert config.getUrlRewriteRules().size() == 3;
    }

    @Test
    public void testPathPrefixes() {
        ExternalServiceConfig config = new ExternalServiceConfig();
        // check we have 2 path prefixes as configured in values.yml
        assert config.getPathPrefixes().size() == 2;
        boolean found = false;
        for(PathPrefix pp: config.getPathPrefixes()) {
            if("/timeout-test".equals(pp.getPathPrefix())) {
                assert "http://localhost:7080".equals(pp.getHost());
                assert 1000 == pp.getTimeout();
                found = true;
            }
        }
        assert found;
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExternalServiceConfigRegisteredForServerInfo() {
        ExternalServiceConfig.load();

        String registryKey = ExternalServiceConfig.CONFIG_NAME + ":" + ExternalServiceHandler.class.getName();
        assertTrue(ModuleRegistry.getModuleRegistry().containsKey(registryKey));

        Map<String, Object> registeredConfig = (Map<String, Object>) ModuleRegistry.getModuleRegistry().get(registryKey);
        assertEquals(Boolean.TRUE, registeredConfig.get("enabled"));

        Map<String, Object> serverInfo = ServerInfoUtil.getServerInfo(ServerInfoConfig.load());
        Map<String, Object> components = (Map<String, Object>) serverInfo.get("component");
        assertTrue(components.containsKey(ExternalServiceConfig.CONFIG_NAME));
    }

    @Test
    public void testOnlyDefaultConfigNameIsCached() {
        ExternalServiceConfig defaultConfig = ExternalServiceConfig.load();
        ExternalServiceConfig testConfig = ExternalServiceConfig.load(ProxyConfig.CONFIG_NAME);

        assertSame(defaultConfig, ExternalServiceConfig.load());
        assertNotSame(testConfig, ExternalServiceConfig.load(ProxyConfig.CONFIG_NAME));
    }
}
