package com.networknt.balance;

import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by dan on 2016-12-29
 */
public class LocalFirstLoadBalanceTest {
    LoadBalance loadBalance = new LocalFirstLoadBalance();

    @Test
    public void testSelect() throws Exception {
        List<URL> urls = new ArrayList<>();
        urls.add(new URLImpl("http", "127.0.0.10", 8081, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.1", 8081, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.11", 8082, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.12", 8083, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.115", 8084, "v1", new HashMap<String, String>()));
        URL url = loadBalance.select(urls);
        Assert.assertEquals(url, URLImpl.valueOf("http://127.0.0.1:8081/v1"));
    }
    
    @Test 
    public void testSelectFirstThenRoundRobin() throws Exception{
        List<URL> urls = new ArrayList<>();
        urls.add(new URLImpl("http", "127.0.0.10", 8081, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.10", 8082, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.10", 8083, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.10", 8084, "v1", new HashMap<String, String>()));

        // no local host URL available, go round-robin
        URL url = loadBalance.select(urls);
        Assert.assertEquals(url, URLImpl.valueOf("http://127.0.0.10:8082/v1"));
    }
    
    @Test
    public void testSelectWithEmptyList() throws Exception {
        List<URL> urls = new ArrayList<>();
        URL url = loadBalance.select(urls);
        Assert.assertNull(url);
    }
}
