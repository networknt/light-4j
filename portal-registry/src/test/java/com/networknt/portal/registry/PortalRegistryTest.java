/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.networknt.portal.registry;

import com.networknt.config.JsonMapper;
import com.networknt.portal.registry.client.PortalRegistryClient;
import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.StringUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PortalRegistryTest {
    private MockPortalRegistryClient client;
    private PortalRegistry registry;
    private URL serviceUrl, serviceUrl2, clientUrl, clientUrl2;
    private long sleepTime;

    @Before
    public void setUp() throws Exception {
        client = (MockPortalRegistryClient)SingletonServiceFactory.getBean(PortalRegistryClient.class);
        registry = (PortalRegistry)SingletonServiceFactory.getBean(Registry.class);

        serviceUrl = MockUtils.getMockUrl(8001);
        serviceUrl2 = MockUtils.getMockUrl(8002);
        clientUrl = MockUtils.getMockUrl("127.0.0.1", 0);
        clientUrl2 = MockUtils.getMockUrl("127.0.0.2", 0);

        sleepTime = PortalRegistryConstants.SWITCHER_CHECK_CIRCLE + 500;
    }

    @After
    public void tearDown() throws Exception {
        registry = null;
        client = null;
    }

    @Test
    public void doRegisterAndAvailable() throws Exception {
        // register
        registry.doRegister(serviceUrl);
        registry.doRegister(serviceUrl2);
        PortalRegistryService service1 = PortalRegistryUtils.buildService(serviceUrl);
        Assert.assertTrue(client.isRegistered(service1));
        Assert.assertFalse(client.isWorking(service1));
        PortalRegistryService service2 = PortalRegistryUtils.buildService(serviceUrl2);
        Assert.assertTrue(client.isRegistered(service2));
        Assert.assertFalse(client.isWorking(service2));

        // available
        registry.doAvailable(null);
        Thread.sleep(sleepTime);
        Assert.assertTrue(client.isWorking(service1));
        Assert.assertTrue(client.isWorking(service2));

        // unavailable
        registry.doUnavailable(null);
        Thread.sleep(sleepTime);
        Assert.assertFalse(client.isWorking(service1));
        Assert.assertFalse(client.isWorking(service2));

        // unregister
        registry.doUnregister(serviceUrl);
        Assert.assertFalse(client.isRegistered(service1));
        Assert.assertTrue(client.isRegistered(service2));
        registry.doUnregister(serviceUrl2);
        Assert.assertFalse(client.isRegistered(service2));
    }

    @Test
    public void subAndUnsubService() throws Exception {
        //registry.doSubscribe(clientUrl, null);
        //registry.doSubscribe(clientUrl2, null);

        registry.doRegister(serviceUrl);
        registry.doRegister(serviceUrl2);
        registry.doAvailable(null);
        Thread.sleep(sleepTime);

        //registry.doUnsubscribe(clientUrl, null);
        //registry.doUnsubscribe(clientUrl2, null);
    }

    @Test
    public void discoverService() throws Exception {
        registry.doRegister(serviceUrl);
        List<URL> urls = registry.doDiscover(serviceUrl);
        Assert.assertTrue(urls.isEmpty());

        registry.doAvailable(null);
        Thread.sleep(sleepTime);
        System.out.println("Before discovery");
        try {
            urls = registry.doDiscover(serviceUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Assert.assertTrue(urls.contains(serviceUrl));
    }

    @Test
    public void testWssUrl() {
        String s = "https://localhost:8438";
        String u = "wss" + s.substring(s.indexOf("://"));
        Assert.assertEquals("wss://localhost:8438", u);

    }

    @Test
    public void testSocketMessage() {
        String s = "{\"com.networknt.ab-1.0.0|test1\":[]}";
        Map<String, Object> map = JsonMapper.string2Map(s);
        // there is only one entry in the map.
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        Map.Entry<String, Object> entry = iterator.next();
        String key = entry.getKey();
        List nodes = (List)entry.getValue();
        Assert.assertEquals(nodes.size(), 0);
        Assert.assertEquals("com.networknt.ab-1.0.0|test1", key);

        String[] parts = StringUtils.split(key, "|");
        String serviceId = parts[0];
        String tag = parts[1];
        Assert.assertEquals("com.networknt.ab-1.0.0", serviceId);
        Assert.assertEquals("test1", tag);
    }

    @Test
    public void testStringSplit() {
        String s = "com.networknt.ab-1.0.0";
        if(s.indexOf("|") > 0) {
            String[] parts = StringUtils.split(s, "|");
            String serviceId = parts[0];
            String tag = parts[1];
            Assert.assertEquals(s, serviceId);
            Assert.assertNull(tag);
        } else {

        }
    }
}