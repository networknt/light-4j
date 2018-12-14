package com.networknt.balance;

import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by steve on 2016-12-07.
 */
public class RoundRobinLoadBalanceTest {
    LoadBalance loadBalance = (LoadBalance) SingletonServiceFactory.getBean(LoadBalance.class);

    @Test
    public void testSelect() throws Exception {
        List<URL> urls = new ArrayList<>();

        urls.add(new URLImpl("http", "127.0.0.1", 8081, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.1", 8082, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.1", 8083, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.1", 8084, "v1", new HashMap<String, String>()));
        while(true) {
            URL url = loadBalance.select(urls, null);
            if(url.getPort() == 8081) break;
        }
        URL url = loadBalance.select(urls, null);
        Assert.assertEquals(url, URLImpl.valueOf("http://127.0.0.1:8082/v1"));
        url = loadBalance.select(urls, null);
        Assert.assertEquals(url, URLImpl.valueOf("http://127.0.0.1:8083/v1"));
        url = loadBalance.select(urls, null);
        Assert.assertEquals(url, URLImpl.valueOf("http://127.0.0.1:8084/v1"));
        url = loadBalance.select(urls, null);
        Assert.assertEquals(url, URLImpl.valueOf("http://127.0.0.1:8081/v1"));
        url = loadBalance.select(urls, null);
        Assert.assertEquals(url, URLImpl.valueOf("http://127.0.0.1:8082/v1"));

    }
    @Test
    public void testSelectWithEmptyList() throws Exception {
        List<URL> urls = new ArrayList<>();
        URL url = loadBalance.select(urls, null);
        Assert.assertNull(url);
    }

    @Test
    public void testRandom() {
        int r1 = (int)(Math.random()*50);
        int r2 = (int)(Math.random()*50);
        System.out.println("r1 = " + r1);
        System.out.println("r2 = " + r2);
        Assert.assertNotEquals(r1, r2);
    }
}
