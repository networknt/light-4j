/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.balance;

import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by steve on 2016-12-07.
 */
public class RoundRobinLoadBalanceTest {
    LoadBalance loadBalance = new RoundRobinLoadBalance();

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

    /**
     * There is a chance that both values are the same but it should be very rare. Given this possibility,
     * ignore this test unless we need it. 
     */
    @Ignore
    @Test
    public void testRandom() {
        int r1 = (int)(Math.random()*50);
        int r2 = (int)(Math.random()*50);
        System.out.println("r1 = " + r1);
        System.out.println("r2 = " + r2);
        Assert.assertNotEquals(r1, r2);
    }
}
