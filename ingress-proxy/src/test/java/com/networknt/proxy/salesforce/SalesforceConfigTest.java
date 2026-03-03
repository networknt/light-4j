package com.networknt.proxy.salesforce;

import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.config.PathPrefixAuth;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class SalesforceConfigTest {
    @Test
    public void testConfigLoad() {
        SalesforceConfig config = SalesforceConfig.load();
        Assertions.assertEquals(3, config.getPathPrefixAuths().size());
        List<PathPrefixAuth> pathPrefixAuthList = config.getPathPrefixAuths();
        Assertions.assertTrue(pathPrefixAuthList.get(0).getAuthAudience() != null);
        Assertions.assertTrue(pathPrefixAuthList.get(0).getAuthIssuer() != null);
        Assertions.assertTrue(pathPrefixAuthList.get(0).getAuthSubject() != null);
        Assertions.assertTrue(pathPrefixAuthList.get(0).getIv() != null);
        Assertions.assertTrue(pathPrefixAuthList.get(0).getServiceHost() != null);
    }

    @Test
    public void testPathPrefixAuths() {
        SalesforceConfig config = SalesforceConfig.load();
        List<PathPrefixAuth> pathPrefixAuthList = config.getPathPrefixAuths();
        Assertions.assertEquals(3, pathPrefixAuthList.size());
    }

    @Test
    public void testUrlRewriteRules() {
        SalesforceConfig config = SalesforceConfig.load();
        List<UrlRewriteRule> urlRewriteRules = config.getUrlRewriteRules();
        Assertions.assertEquals(2, urlRewriteRules.size());
    }
}
