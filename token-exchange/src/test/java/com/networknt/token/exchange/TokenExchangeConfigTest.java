/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.networknt.token.exchange;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

/**
 * Unit tests for TokenExchangeConfig.
 *
 * @author Steve Hu
 */
public class TokenExchangeConfigTest {

    private TokenExchangeConfig config;

    @Before
    public void setUp() {
        config = TokenExchangeConfig.load();
    }

    @Test
    public void testLoadConfig() {
        Assert.assertNotNull(config);
        Assert.assertNotNull(config.getMappedConfig());
    }

    @Test
    public void testEnabled() {
        Assert.assertTrue(config.isEnabled());
    }

    @Test
    public void testTokenExUri() {
        Assert.assertNotNull(config.getTokenExUri());
        Assert.assertEquals("https://localhost:6882/oauth2/token", config.getTokenExUri());
    }

    @Test
    public void testTokenExClientId() {
        Assert.assertNotNull(config.getTokenExClientId());
        Assert.assertEquals("portal-client", config.getTokenExClientId());
    }

    @Test
    public void testTokenExClientSecret() {
        Assert.assertNotNull(config.getTokenExClientSecret());
        Assert.assertEquals("portal-secret", config.getTokenExClientSecret());
    }

    @Test
    public void testTokenExScope() {
        List<String> scope = config.getTokenExScope();
        Assert.assertNotNull(scope);
        Assert.assertEquals(2, scope.size());
        Assert.assertTrue(scope.contains("portal.w"));
        Assert.assertTrue(scope.contains("portal.r"));
    }

    @Test
    public void testSubjectTokenType() {
        Assert.assertNotNull(config.getSubjectTokenType());
        Assert.assertEquals("urn:ietf:params:oauth:token-type:jwt", config.getSubjectTokenType());
    }

    @Test
    public void testRequestedTokenType() {
        Assert.assertNotNull(config.getRequestedTokenType());
        Assert.assertEquals("urn:ietf:params:oauth:token-type:jwt", config.getRequestedTokenType());
    }

    @Test
    public void testMappingStrategy() {
        Assert.assertNotNull(config.getMappingStrategy());
        Assert.assertEquals("database", config.getMappingStrategy());
    }

    @Test
    public void testClientMappingIsNull() {
        // clientMapping is not defined in the test config, so it should be null
        Map<String, String> clientMapping = config.getClientMapping();
        Assert.assertNull(clientMapping);
    }

    @Test
    public void testConfigWithClientMapping() {
        // Load an alternative config with clientMapping defined
        TokenExchangeConfig altConfig = TokenExchangeConfig.load("token-exchange-mapping");
        Assert.assertNotNull(altConfig);
        Assert.assertEquals("config", altConfig.getMappingStrategy());
        Map<String, String> clientMapping = altConfig.getClientMapping();
        Assert.assertNotNull(clientMapping);
        Assert.assertEquals("internal-function-1", clientMapping.get("external-client-1"));
        Assert.assertEquals("internal-function-2", clientMapping.get("external-client-2"));
    }
}
