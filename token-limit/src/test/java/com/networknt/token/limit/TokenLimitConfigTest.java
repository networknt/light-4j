package com.networknt.token.limit;

import com.networknt.cache.CacheManager;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class TokenLimitConfigTest {
    private static TokenLimitConfig config;

    @Before
    public void setUp() {
        config = TokenLimitConfig.load("token-limit-template");
    }

    @Test
    public void testConfigData() {
        Assert.assertTrue(config.isEnabled());
        Assert.assertTrue(config.isErrorOnLimit());
        Assert.assertEquals(2, config.getDuplicateLimit());
        Assert.assertTrue(config.getTokenPathTemplates().contains("/oauth2/(?<instanceId>[^/]+)/v1/token"));
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
