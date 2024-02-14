package com.networknt.proxy;

import org.junit.Test;

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
        assert config.getUrlRewriteRules().size() == 2;
    }
}
