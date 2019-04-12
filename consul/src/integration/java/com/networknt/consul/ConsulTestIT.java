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

package com.networknt.consul;

import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import org.junit.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConsulTestIT {
    private ConsulRegistry registry;
    private URL serviceUrl;
    private long sleepTime;

    @Before
    public void setUp() throws Exception {
        registry = (ConsulRegistry)SingletonServiceFactory.getBean(Registry.class);


        serviceUrl = getMockUrl("http", "192.168.1.119",8083, "MockService");

        sleepTime = ConsulConstants.SWITCHER_CHECK_CIRCLE + 500;
    }

    @After
    public void tearDown() throws Exception {
        registry = null;
    }

    @Test
    @Ignore
    public void doRegisterAndAvailable() throws Exception {
        // register
        registry.doRegister(serviceUrl);

        // unregister
        registry.doUnregister(serviceUrl);
    }

    @Test
    @Ignore
    public void discoverService() throws Exception {
        registry.doRegister(serviceUrl);
        List<URL> urls = registry.discoverService(serviceUrl);
        Assert.assertNull(urls);
        Thread.sleep(sleepTime);
        urls = registry.discoverService(serviceUrl);
        //Assert.assertTrue(urls.contains(serviceUrl));
    }

    public static URL getMockUrl(String protocol, String address, int port, String serviceName) {
        Map<String, String> params = new HashMap<>();
        params.put("environment", "test1");
        URL url = new URLImpl(protocol, address, port, serviceName, params);
        return url;
    }
}
