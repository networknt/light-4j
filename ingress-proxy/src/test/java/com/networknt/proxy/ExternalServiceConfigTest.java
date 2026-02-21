package com.networknt.proxy;

import org.junit.jupiter.api.Test;

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
}
