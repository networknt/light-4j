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
        String s = cluster.serviceToUrl("http", "code");
        Assert.assertEquals(s, "http://192.168.1.101:6881");

        s = cluster.serviceToUrl("http", "code");
        Assert.assertEquals(s, "http://192.168.1.100:6881");
    }
}
