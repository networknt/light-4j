package com.networknt.balance;

import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by steve on 2016-12-07.
 */
public class RoundRobinLoadBalanceTest {
    LoadBalance loadBalance = new RoundRobinLoadBalance();

    @Test
    public void testSelect() throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(new URL("http://127.0.0.1:8081/v1"));
        urls.add(new URL("http://127.0.0.1:8082/v1"));
        urls.add(new URL("http://127.0.0.1:8083/v1"));
        urls.add(new URL("http://127.0.0.1:8084/v1"));
        URL url = loadBalance.select(urls);
        Assert.assertEquals(url, new URL("http://127.0.0.1:8082/v1"));
        url = loadBalance.select(urls);
        Assert.assertEquals(url, new URL("http://127.0.0.1:8083/v1"));
        url = loadBalance.select(urls);
        Assert.assertEquals(url, new URL("http://127.0.0.1:8084/v1"));
        url = loadBalance.select(urls);
        Assert.assertEquals(url, new URL("http://127.0.0.1:8081/v1"));
        url = loadBalance.select(urls);
        Assert.assertEquals(url, new URL("http://127.0.0.1:8082/v1"));

    }
    @Test
    public void testSelectWithEmptyList() throws Exception {
        List<URL> urls = new ArrayList<>();
        URL url = loadBalance.select(urls);
        Assert.assertNull(url);
    }
}
