package com.networknt.cors;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

public class CorsConfigTest {
    @Test
    public void testCorsConfig() {
        CorsConfig config = CorsConfig.load();
        Assert.assertTrue(config.isEnabled());
        List allowedOrigins = config.getAllowedOrigins();
        Assert.assertEquals(3, allowedOrigins.size());
        List allowedMethods = config.getAllowedMethods();
        Assert.assertEquals(5, allowedMethods.size());
        Map pathPrefixAllowed = config.getPathPrefixAllowed();
        Assert.assertEquals(2, pathPrefixAllowed.size());
        Map petstoreMap = (Map) pathPrefixAllowed.get("/v1/pets");
        Assert.assertEquals(2, petstoreMap.size());
        List petstoreAllowedOrigins = (List) petstoreMap.get(CorsConfig.ALLOWED_ORIGINS);
        Assert.assertEquals(2, petstoreAllowedOrigins.size());
        List petstoreAllowedMethods = (List) petstoreMap.get(CorsConfig.ALLOWED_METHODS);
        Assert.assertEquals(4, petstoreAllowedMethods.size());
        Map marketMap = (Map) pathPrefixAllowed.get("/v1/market");
        Assert.assertEquals(2, marketMap.size());
        List marketAllowedOrigins = (List) marketMap.get(CorsConfig.ALLOWED_ORIGINS);
        Assert.assertEquals(2, marketAllowedOrigins.size());
        List marketAllowedMethods = (List) marketMap.get(CorsConfig.ALLOWED_METHODS);
        Assert.assertEquals(2, marketAllowedMethods.size());
    }

    @Test
    public void testPathPrefixAllowed() {
        CorsConfig config = CorsConfig.load();
        String requestPath = "/v1/pets/1";
        List<String> allowedOrigins = config.getAllowedOrigins();
        List<String> allowedMethods = config.getAllowedMethods();

        Assert.assertEquals(3, allowedOrigins.size());
        Assert.assertEquals(5, allowedMethods.size());

        for(Map.Entry<String, Object> entry: config.getPathPrefixAllowed().entrySet()) {
            if (requestPath.startsWith(entry.getKey())) {
                Map endpointCorsMap = (Map) entry.getValue();
                allowedOrigins = (List<String>) endpointCorsMap.get(CorsConfig.ALLOWED_ORIGINS);
                allowedMethods = (List<String>) endpointCorsMap.get(CorsConfig.ALLOWED_METHODS);
                break;
            }
        }
        Assert.assertEquals(2, allowedOrigins.size());
        Assert.assertEquals(4, allowedMethods.size());

    }
}
