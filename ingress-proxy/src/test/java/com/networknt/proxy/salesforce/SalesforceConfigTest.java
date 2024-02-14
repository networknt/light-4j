package com.networknt.proxy.salesforce;

import com.networknt.handler.config.UrlRewriteRule;
import com.networknt.proxy.PathPrefixAuth;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SalesforceConfigTest {
    @Test
    public void testConfigLoad() {
        SalesforceConfig config = SalesforceConfig.load();
        Assert.assertEquals(3, config.getPathPrefixAuths().size());
        List<PathPrefixAuth> pathPrefixAuthList = config.getPathPrefixAuths();
        Assert.assertTrue(pathPrefixAuthList.get(0).getAuthAudience() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getAuthIssuer() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getAuthSubject() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getIv() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getServiceHost() != null);
    }

    @Test
    public void testPathPrefixAuths() {
        SalesforceConfig config = SalesforceConfig.load();
        List<PathPrefixAuth> pathPrefixAuthList = config.getPathPrefixAuths();
        Assert.assertEquals(3, pathPrefixAuthList.size());
    }

    @Test
    public void testUrlRewriteRules() {
        SalesforceConfig config = SalesforceConfig.load();
        List<UrlRewriteRule> urlRewriteRules = config.getUrlRewriteRules();
        Assert.assertEquals(2, urlRewriteRules.size());
    }
}
