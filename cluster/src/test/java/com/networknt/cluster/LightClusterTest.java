package com.networknt.cluster;

import com.networknt.service.SingletonServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.List;

/**
 * Created by stevehu on 2017-01-27.
 */
public class LightClusterTest {
    private static Cluster cluster = (Cluster) SingletonServiceFactory.getBean(Cluster.class);

    @Test
    public void testServiceToUrl() {
        String s = cluster.serviceToUrl("http", "com.networknt.apib-1.0.0", null, null);
        Assert.assertTrue("http://localhost:7005".equals(s) || "http://localhost:7002".equals(s));
        s = cluster.serviceToUrl("http", "com.networknt.apib-1.0.0", null, null);
        Assert.assertTrue("http://localhost:7005".equals(s) || "http://localhost:7002".equals(s));
    }

    @Test
    public void testServices() {
        List<URI> l = cluster.services("http", "com.networknt.apib-1.0.0", null);
        Assert.assertEquals(2, l.size());
    }
}
