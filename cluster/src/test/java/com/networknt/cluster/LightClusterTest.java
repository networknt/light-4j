package com.networknt.cluster;

import com.networknt.service.SingletonServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by stevehu on 2017-01-27.
 */
public class LightClusterTest {
    private static Cluster cluster = (Cluster) SingletonServiceFactory.getBean(Cluster.class);

    @Test
    public void testLightCluster() {
        String s = cluster.serviceToUrl("http", "com.networknt.apib-1.0.0", null, null);
        Assert.assertEquals(s, "http://localhost:7005");

        s = cluster.serviceToUrl("http", "com.networknt.apib-1.0.0", null, null);
        Assert.assertEquals(s, "http://localhost:7002");
    }
}
