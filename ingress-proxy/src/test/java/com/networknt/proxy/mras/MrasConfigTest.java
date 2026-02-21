package com.networknt.proxy.mras;

import com.networknt.common.ContentType;
import com.networknt.handler.config.UrlRewriteRule;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

public class MrasConfigTest {
    @Test
    @Disabled
    public void testConfigLoad() {
        MrasConfig config = MrasConfig.load();
        Assertions.assertEquals(4, config.getPathPrefixAuth().size());
        Assertions.assertTrue(config.getAccessToken().size() > 0);
        Assertions.assertTrue(config.getBasicAuth().size() > 0);
        Assertions.assertTrue(config.getAnonymous().size() > 0);
        Assertions.assertTrue(config.getMicrosoft().size() > 0);

        Assertions.assertTrue(config.getMicrosoft().get(config.SERVICE_HOST) != null);
        Assertions.assertTrue(config.getAccessToken().get(config.SERVICE_HOST) != null);
        Assertions.assertTrue(config.getAnonymous().get(config.SERVICE_HOST) != null);
        Assertions.assertTrue(config.getBasicAuth().get(config.SERVICE_HOST) != null);
    }

    @Test
    public void testContentType() {
        String s = ContentType.APPLICATION_JSON.value();
        System.out.println("s = " + s);
    }

    @Test
    public void testPathPrefixAuths() {
        MrasConfig config = MrasConfig.load();
        Map<String, Object> pathPrefixAuthList = config.getPathPrefixAuth();
        Assertions.assertEquals(4, pathPrefixAuthList.size());
    }

    @Test
    public void testUrlRewriteRules() {
        MrasConfig config = MrasConfig.load();
        List<UrlRewriteRule> urlRewriteRules = config.getUrlRewriteRules();
        Assertions.assertEquals(2, urlRewriteRules.size());
    }

}
