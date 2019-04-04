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
import com.networknt.service.SingletonServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * Created by stevehu on 2017-01-18.
 */
public class DirectRegistryTest {
    @Test
    public void testDirectRegistry() {
        Registry registry = SingletonServiceFactory.getBean(Registry.class);

        URL subscribeUrl = URLImpl.valueOf("light://localhost:8080/token");
        List<URL> urls = registry.discover(subscribeUrl);
        Assert.assertEquals(1, urls.size());

        subscribeUrl = URLImpl.valueOf("light://localhost:8080/code");
        urls = registry.discover(subscribeUrl);
        Assert.assertEquals(2, urls.size());


    }
}
