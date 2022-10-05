package com.networknt.proxy.conquest;

import com.networknt.proxy.PathPrefixAuth;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class ConquestConfigTest {
    @Test
    public void testConfigLoad() {
        ConquestConfig config = ConquestConfig.load();
        Assert.assertEquals(1, config.getPathPrefixAuths().size());
        List<PathPrefixAuth> pathPrefixAuthList = config.getPathPrefixAuths();
        Assert.assertTrue(pathPrefixAuthList.get(0).getAuthAudience() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getAuthIssuer() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getAuthSubject() != null);
        Assert.assertTrue(pathPrefixAuthList.get(0).getServiceHost() != null);
    }

}
