package com.networknt.token.limit;

import com.networknt.cache.CacheManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TokenLimitConfigTest {
    private static TokenLimitConfig config;

    @BeforeEach
    public void setUp() {
        config = TokenLimitConfig.load("token-limit-template");
    }

    @Test
    public void testConfigData() {
        Assertions.assertTrue(config.isEnabled());
        Assertions.assertTrue(config.isErrorOnLimit());
        Assertions.assertEquals(2, config.getDuplicateLimit().intValue());
        Assertions.assertEquals("expires_in", config.getExpireKey());
        Assertions.assertTrue(config.getTokenPathTemplates().contains("/oauth2/(?<instanceId>[^/]+)/v1/token"));
    }

    @Test
    public void testRegexReplace() {
        String urlPattern = "/api/users/{userId}/posts/{postId}";
        String regexPattern = urlPattern
                .replaceAll("\\{([^{}]+)\\}", "(?<$1>[^/]+)");
        System.out.println("regexPattern = " + regexPattern);
    }

    @Test
    public void testRegexReplace2() {
        CacheManager cacheManager = CacheManager.getInstance();
        System.out.println("cacheManager = " + cacheManager);
        cacheManager = CacheManager.getInstance();
        System.out.println("cacheManager = " + cacheManager);
    }
}
