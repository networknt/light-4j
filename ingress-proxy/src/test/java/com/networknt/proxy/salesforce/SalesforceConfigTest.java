package com.networknt.proxy.salesforce;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class SalesforceConfigTest {
    @Test
    public void testConfigLoad() {
        SalesforceConfig config = SalesforceConfig.load();
        Assert.assertEquals(2, config.getPathPrefixAuths().size());
        List<PathPrefixAuth> pathPrefixAuthList = config.getPathPrefixAuths();
        Assert.assertTrue(pathPrefixAuthList.get(0).getAuthAudience() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getAuthIssuer() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getAuthSubject() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getIv() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getServiceHost() != null);
    }
}
