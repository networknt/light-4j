package com.networknt.proxy.salesforce;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class SalesforceConfigTest {
    @Test
    @Ignore
    public void testConfigLoad() {
        SalesforceConfig config = SalesforceConfig.load();
        Assert.assertEquals(3, config.getPathPrefixAuth().size());
        Assert.assertTrue(config.getMorningStar().size()  == 3);
        Assert.assertTrue(config.getConquest().size() == 3);
        Assert.assertTrue(config.getAdvisorHub().size() == 3);

        Assert.assertTrue(config.getMorningStar().get(config.IV) != null);
        Assert.assertTrue(config.getConquest().get(config.IV) != null);
        Assert.assertTrue(config.getAdvisorHub().get(config.IV) != null);
    }

}
