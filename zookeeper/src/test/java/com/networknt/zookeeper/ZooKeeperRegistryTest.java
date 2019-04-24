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

package com.networknt.zookeeper;

import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.registry.support.command.CommandListener;
import com.networknt.registry.support.command.ServiceListener;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.Constants;
import com.networknt.zookeeper.client.ZooKeeperClient;
import org.apache.curator.test.InstanceSpec;
import org.apache.curator.test.TestingServer;
import org.junit.*;

import java.util.HashMap;
import java.util.List;

public class ZooKeeperRegistryTest {
    private static ZooKeeperRegistry registry;
    private static URL serviceUrl, clientUrl;
    private static ZooKeeperClient client;
    private static String service = "com.networknt.light.demoService";
    private static TestingServer zookeeper;

    @BeforeClass
    public static void setUp() throws Exception
    {
        int port = 9000;
        clientUrl = new URLImpl(Constants.PROTOCOL_LIGHT, "127.0.0.1", 0, service);
        clientUrl.addParameter("group", "aaa");

        serviceUrl = new URLImpl(Constants.PROTOCOL_LIGHT, "127.0.0.1", 8001, service);
        serviceUrl.addParameter("group", "aaa");

        InstanceSpec spec = new InstanceSpec(null, port, -1, -1, true, 1,-1, -1,new HashMap<>());
        zookeeper = new TestingServer(spec, true);

        client = (ZooKeeperClient)SingletonServiceFactory.getBean(ZooKeeperClient.class);
        registry = (ZooKeeperRegistry)SingletonServiceFactory.getBean(Registry.class);
        System.out.println("client = " + client + " registry = " + registry);
    }

    @AfterClass
    public static void tearDown() throws Exception
    {
        zookeeper.stop();
    }

    @Test
    public void subAndUnsubService() throws Exception {
        ServiceListener serviceListener = new ServiceListener() {
            @Override
            public void notifyService(URL refUrl, URL registryUrl, List<URL> urls) {
                if (!urls.isEmpty()) {
                    Assert.assertTrue(urls.contains(serviceUrl));
                }
            }
        };
        registry.subscribeService(clientUrl, serviceListener);
        Assert.assertTrue(containsServiceListener(clientUrl, serviceListener));
        registry.doRegister(serviceUrl);
        registry.doAvailable(serviceUrl);
        Thread.sleep(2000);

        registry.unsubscribeService(clientUrl, serviceListener);
        Assert.assertFalse(containsServiceListener(clientUrl, serviceListener));
    }

    private boolean containsServiceListener(URL clientUrl, ServiceListener serviceListener) {
        return registry.getServiceListeners().get(clientUrl).containsKey(serviceListener);
    }

    @Test
    public void discoverService() throws Exception {
        registry.doRegister(serviceUrl);
        List<URL> results = registry.discoverService(clientUrl);
        Assert.assertTrue(results.isEmpty());

        registry.doAvailable(serviceUrl);
        results = registry.discoverService(clientUrl);
        Assert.assertTrue(results.contains(serviceUrl));
    }

    @Test
    public void doRegisterAndAvailable() throws Exception {
        String node = serviceUrl.getServerPortStr();
        List<String> available, unavailable;
        String unavailablePath = ZkUtils.toNodeTypePath(serviceUrl, ZkNodeType.UNAVAILABLE_SERVER);
        String availablePath = ZkUtils.toNodeTypePath(serviceUrl, ZkNodeType.AVAILABLE_SERVER);

        registry.doRegister(serviceUrl);
        unavailable = client.getChildren(unavailablePath);
        Assert.assertTrue(unavailable.contains(node));

        registry.doAvailable(serviceUrl);
        unavailable = client.getChildren(unavailablePath);
        Assert.assertFalse(unavailable.contains(node));
        available = client.getChildren(availablePath);
        Assert.assertTrue(available.contains(node));

        registry.doUnavailable(serviceUrl);
        unavailable = client.getChildren(unavailablePath);
        Assert.assertTrue(unavailable.contains(node));
        available = client.getChildren(availablePath);
        Assert.assertFalse(available.contains(node));

        registry.doUnregister(serviceUrl);
        unavailable = client.getChildren(unavailablePath);
        Assert.assertFalse(unavailable.contains(node));
        available = client.getChildren(availablePath);
        Assert.assertFalse(available.contains(node));
    }

}
