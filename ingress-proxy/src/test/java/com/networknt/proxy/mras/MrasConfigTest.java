package com.networknt.proxy.mras;

import com.networknt.httpstring.ContentType;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class MrasConfigTest {
    @Test
    @Ignore
    public void testConfigLoad() {
        MrasConfig config = MrasConfig.load();
        Assert.assertEquals(4, config.getPathPrefixAuth().size());
        Assert.assertTrue(config.getAccessToken().size() > 0);
        Assert.assertTrue(config.getBasicAuth().size() > 0);
        Assert.assertTrue(config.getAnonymous().size() > 0);
        Assert.assertTrue(config.getMicrosoft().size() > 0);

        Assert.assertTrue(config.getMicrosoft().get(config.SERVICE_HOST) != null);
        Assert.assertTrue(config.getAccessToken().get(config.SERVICE_HOST) != null);
        Assert.assertTrue(config.getAnonymous().get(config.SERVICE_HOST) != null);
        Assert.assertTrue(config.getBasicAuth().get(config.SERVICE_HOST) != null);
    }

    @Test
    public void testContentType() {
        String s = ContentType.APPLICATION_JSON.value();
        System.out.println("s = " + s);
    }
}
