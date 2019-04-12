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

package com.networknt.consul;

import com.networknt.consul.client.ConsulClient;
import com.networknt.registry.Registry;
import com.networknt.registry.URLImpl;
import com.networknt.registry.URLParamType;
import com.networknt.registry.support.command.CommandListener;
import com.networknt.registry.support.command.ServiceListener;
import com.networknt.registry.URL;
import com.networknt.service.SingletonServiceFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.logging.ConsoleHandler;

public class ConsulRegistryTest {
    private MockConsulClient client;
    private ConsulRegistry registry;
    private URL serviceUrl, serviceUrl2, clientUrl, clientUrl2;
    private String serviceid, serviceid2;
    private long sleepTime;

    @Before
    public void setUp() throws Exception {
        client = (MockConsulClient)SingletonServiceFactory.getBean(ConsulClient.class);
        registry = (ConsulRegistry)SingletonServiceFactory.getBean(Registry.class);

        serviceUrl = MockUtils.getMockUrl(8001);
        serviceUrl2 = MockUtils.getMockUrl(8002);
        serviceid = ConsulUtils.convertConsulSerivceId(serviceUrl);
        serviceid2 = ConsulUtils.convertConsulSerivceId(serviceUrl2);
        clientUrl = MockUtils.getMockUrl("127.0.0.1", 0);
        clientUrl2 = MockUtils.getMockUrl("127.0.0.2", 0);

        sleepTime = ConsulConstants.SWITCHER_CHECK_CIRCLE + 500;
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
        Assert.assertTrue(client.isRegistered(serviceid));
        Assert.assertFalse(client.isWorking(serviceid));
        Assert.assertTrue(client.isRegistered(serviceid2));
        Assert.assertFalse(client.isWorking(serviceid2));

        // available
        registry.doAvailable(null);
        Thread.sleep(sleepTime);
        Assert.assertTrue(client.isWorking(serviceid));
        Assert.assertTrue(client.isWorking(serviceid2));

        // unavailable
        registry.doUnavailable(null);
        Thread.sleep(sleepTime);
        Assert.assertFalse(client.isWorking(serviceid));
        Assert.assertFalse(client.isWorking(serviceid2));

        // unregister
        registry.doUnregister(serviceUrl);
        Assert.assertFalse(client.isRegistered(serviceid));
        Assert.assertTrue(client.isRegistered(serviceid2));
        registry.doUnregister(serviceUrl2);
        Assert.assertFalse(client.isRegistered(serviceid2));
    }

    private ServiceListener createNewServiceListener(final URL serviceUrl) {
        return new ServiceListener() {
            @Override
            public void notifyService(URL refUrl, URL registryUrl, List<URL> urls) {
                if (!urls.isEmpty()) {
                    Assert.assertTrue(urls.contains(serviceUrl));
                }
            }
        };
    }

    @Test
    public void subAndUnsubService() throws Exception {
        ServiceListener serviceListener = createNewServiceListener(serviceUrl);
        ServiceListener serviceListener2 = createNewServiceListener(serviceUrl);

        registry.subscribeService(clientUrl, serviceListener);
        registry.subscribeService(clientUrl2, serviceListener2);
        Assert.assertTrue(containsServiceListener(serviceUrl, clientUrl, serviceListener));
        Assert.assertTrue(containsServiceListener(serviceUrl, clientUrl2, serviceListener2));

        registry.doRegister(serviceUrl);
        registry.doRegister(serviceUrl2);
        registry.doAvailable(null);
        Thread.sleep(sleepTime);

        registry.unsubscribeService(clientUrl, serviceListener);
        Assert.assertFalse(containsServiceListener(serviceUrl, clientUrl, serviceListener));
        Assert.assertTrue(containsServiceListener(serviceUrl, clientUrl2, serviceListener2));

        registry.unsubscribeService(clientUrl2, serviceListener2);
        Assert.assertFalse(containsServiceListener(serviceUrl, clientUrl2, serviceListener2));

    }

    @Test
    public void discoverService() throws Exception {
        registry.doRegister(serviceUrl);
        List<URL> urls = registry.discoverService(serviceUrl);
        Assert.assertNull(urls);

        registry.doAvailable(null);
        Thread.sleep(sleepTime);
        urls = registry.discoverService(serviceUrl);
        Assert.assertTrue(urls.contains(serviceUrl));
    }

    private Boolean containsServiceListener(URL serviceUrl, URL clientUrl, ServiceListener serviceListener) {
        String service = ConsulUtils.getUrlClusterInfo(serviceUrl);
        return registry.getServiceListeners().get(service).get(clientUrl) == serviceListener;
    }

}