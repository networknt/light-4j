package com.networknt.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class UnifiedSecurityConfigTest {
    @Test
    public void testLoadConfig() {
        UnifiedSecurityConfig config = UnifiedSecurityConfig.load();
        Assertions.assertTrue(config.isEnabled());
        // check anonymousPrefixes have /v1/pets
        List<String> anonymousPrefixes = config.getAnonymousPrefixes();
        Assertions.assertTrue(anonymousPrefixes.contains("/v1/dogs"));
        Assertions.assertTrue(anonymousPrefixes.contains("/v1/cats"));
        Assertions.assertEquals(3, config.getAnonymousPrefixes().size());
        // check the pathPrefixAuths
        Assertions.assertEquals(4, config.getPathPrefixAuths().size());
        UnifiedPathPrefixAuth auth1 = config.getPathPrefixAuths().get(0);
        Assertions.assertTrue(auth1.isBasic());
        Assertions.assertTrue(auth1.isJwt());
        Assertions.assertTrue(auth1.isApikey());
        Assertions.assertEquals(2, auth1.getJwkServiceIds().size());
        Assertions.assertEquals("com.networknt.market-1.0.0", auth1.getJwkServiceIds().get(1));
        UnifiedPathPrefixAuth auth2 = config.getPathPrefixAuths().get(1);
        Assertions.assertTrue(auth2.isBasic());
        Assertions.assertTrue(auth2.isJwt());
        Assertions.assertFalse(auth2.isApikey());
        Assertions.assertEquals(2, auth2.getJwkServiceIds().size());
        Assertions.assertEquals("com.networknt.market-1.0.0", auth2.getJwkServiceIds().get(1));
    }

    @Test
    public void testLoadStringConfig() {
        UnifiedSecurityConfig config = UnifiedSecurityConfig.load("unified-security-json");
        Assertions.assertTrue(config.isEnabled());
        // check anonymousPrefixes have /v1/pets
        List<String> anonymousPrefixes = config.getAnonymousPrefixes();
        Assertions.assertTrue(anonymousPrefixes.contains("/v1/pets"));
        Assertions.assertTrue(anonymousPrefixes.contains("/v1/cats"));
        Assertions.assertEquals(2, config.getAnonymousPrefixes().size());
        // check the pathPrefixAuths
        Assertions.assertEquals(2, config.getPathPrefixAuths().size());
        UnifiedPathPrefixAuth auth1 = config.getPathPrefixAuths().get(0);
        Assertions.assertTrue(auth1.isBasic());
        Assertions.assertTrue(auth1.isJwt());
        Assertions.assertTrue(auth1.isApikey());
        Assertions.assertEquals(2, auth1.getJwkServiceIds().size());
        Assertions.assertEquals("com.networknt.market-1.0.0", auth1.getJwkServiceIds().get(1));
        UnifiedPathPrefixAuth auth2 = config.getPathPrefixAuths().get(1);
        Assertions.assertTrue(auth2.isBasic());
        Assertions.assertTrue(auth2.isJwt());
        Assertions.assertFalse(auth2.isApikey());
        Assertions.assertEquals(2, auth2.getJwkServiceIds().size());
        Assertions.assertEquals("com.networknt.market-1.0.0", auth2.getJwkServiceIds().get(1));
    }

    @Test
    public void testLoadNoListConfig() {
        UnifiedSecurityConfig config = UnifiedSecurityConfig.load("unified-security-nolist");
        Assertions.assertTrue(config.isEnabled());
        // check anonymousPrefixes have /v1/pets
        List<String> anonymousPrefixes = config.getAnonymousPrefixes();
        Assertions.assertNull(anonymousPrefixes);
        // check the pathPrefixAuths
        Assertions.assertNull(config.getPathPrefixAuths());
    }

    @Test
    public void testPathPrefixAuth() {
        String s = "[{\"prefix\":\"/adm/modules\",\"basic\":true},{\"prefix\":\"/adm/server\",\"basic\":true},{\"prefix\":\"/adm/logger\",\"basic\":true},{\"prefix\":\"/adm/health\",\"basic\":true},{\"prefix\":\"/gateway/NavigationTP\",\"jwt\":true,\"jwkServiceIds\":\"Navigation, NavigationJWT\"}]";
        if(s.startsWith("[")) {
            // json format
            try {
                List<Map<String, Object>> values = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {});
                Assertions.assertEquals(5, values.size());
                List<UnifiedPathPrefixAuth> pathPrefixAuths = UnifiedSecurityConfig.populatePathPrefixAuths(values);
                Assertions.assertEquals(5, pathPrefixAuths.size());
            } catch (Exception e) {
                e.printStackTrace();
                throw new ConfigException("could not parse the pathPrefixAuths json with a list of string and object.");
            }
        } else {
            throw new ConfigException("pathPrefixAuths must be a list of string object map.");
        }
    }
}
