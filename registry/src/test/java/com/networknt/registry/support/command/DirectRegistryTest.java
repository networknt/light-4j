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
package com.networknt.registry.support.command;

import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.registry.support.DirectRegistry;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by stevehu on 2017-01-18.
 */
public class DirectRegistryTest {
    @Test
    public void testDirectRegistry() {
        Registry registry = SingletonServiceFactory.getBean(Registry.class);

        URL subscribeUrl = URLImpl.valueOf("light://localhost:7080/token");
        List<URL> urls = registry.discover(subscribeUrl);
        Assertions.assertEquals(1, urls.size());

        subscribeUrl = URLImpl.valueOf("light://localhost:7080/code");
        urls = registry.discover(subscribeUrl);
        Assertions.assertEquals(2, urls.size());


    }

    @Test
    public void testDirectRegistryWithEnvironment() {
        Registry registry = SingletonServiceFactory.getBean(Registry.class);

        URL subscribeUrl = URLImpl.valueOf("light://localhost:7080/command?environment=0000");

        List<URL> urls = registry.discover(subscribeUrl);
        Assertions.assertEquals(1, urls.size());
        Assertions.assertTrue(urls.get(0).getPort() == 8440);

        subscribeUrl = URLImpl.valueOf("light://localhost:7080/command?environment=0001");
        urls = registry.discover(subscribeUrl);
        Assertions.assertEquals(1, urls.size());
        Assertions.assertTrue(urls.get(0).getPort() == 8441);

        subscribeUrl = URLImpl.valueOf("light://localhost:7080/command?environment=0002");
        urls = registry.discover(subscribeUrl);
        Assertions.assertEquals(1, urls.size());
        Assertions.assertTrue(urls.get(0).getPort() == 8442);
    }

    @Test
    public void testDirectRegistryFromConfigWithBasePath() {
        Registry registry = SingletonServiceFactory.getBean(Registry.class);

        URL subscribeUrl = URLImpl.valueOf("light://localhost:7080/com.networknt.ingress-1.0.0");
        List<URL> urls = registry.discover(subscribeUrl);
        Assertions.assertEquals(1, urls.size());
        Assertions.assertEquals("api.example.com", urls.get(0).getHost());
        Assertions.assertEquals("/namespace1/service1", urls.get(0).getParameter(Constants.BASE_PATH));
    }

    @Test
    public void testDirectRegistryFromParametersWithBasePath() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("com.networknt.param-1.0.0", "https://api.example.com/namespace1/service1");
        parameters.put("com.networknt.paramtag-1.0.0", "https://api.example.com/namespace2/service2?environment=0000");
        Registry registry = new DirectRegistry(new URLImpl("direct", "localhost", 8080, "direct", parameters));

        List<URL> urls = registry.discover(URLImpl.valueOf("light://localhost:7080/com.networknt.param-1.0.0"));
        Assertions.assertEquals(1, urls.size());
        URL url = urls.get(0);
        Assertions.assertEquals("api.example.com", url.getHost());
        Assertions.assertEquals(443, url.getPort());
        Assertions.assertEquals("/namespace1/service1", url.getParameter(Constants.BASE_PATH));
        Assertions.assertEquals("com.networknt.param-1.0.0", url.getPath());

        urls = registry.discover(URLImpl.valueOf("light://localhost:7080/com.networknt.paramtag-1.0.0?environment=0000"));
        Assertions.assertEquals(1, urls.size());
        url = urls.get(0);
        Assertions.assertEquals("/namespace2/service2", url.getParameter(Constants.BASE_PATH));
        Assertions.assertEquals("0000", url.getParameter(Constants.TAG_ENVIRONMENT));
    }

}
