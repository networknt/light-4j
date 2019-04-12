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
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by steve on 08/05/17.
 */
public class ConsitentHashLoadBalanceTest {
    LoadBalance loadBalance = new ConsistentHashLoadBalance();

    @Test
    public void testSelect() throws Exception {
        List<URL> urls = new ArrayList<>();

        //TODO complete the test cases when the class is completed. Need implementation.
        /*
        urls.add(new URLImpl("http", "127.0.0.1", 8081, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.1", 8082, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.1", 8083, "v1", new HashMap<String, String>()));
        urls.add(new URLImpl("http", "127.0.0.1", 8084, "v1", new HashMap<String, String>()));

        URL url1 = loadBalance.select(urls, "user1");
        URL url2 = loadBalance.select(urls, "user1");
        Assert.assertEquals(url1, url2);

        URL url3 = loadBalance.select(urls, "user1");
        Assert.assertEquals(url1, url3);


        URL url4 = loadBalance.select(urls, "user2");
        Assert.assertNotEquals(url1, url4);
        URL url5 = loadBalance.select(urls, "user3");
        Assert.assertNotEquals(url4, url5);
        */
    }
}
