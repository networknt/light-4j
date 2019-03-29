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

package com.networknt.cluster;

import com.networknt.service.SingletonServiceFactory;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.util.List;

/**
 * Created by stevehu on 2017-01-27.
 */
public class LightClusterTest {
    private static Cluster cluster = (Cluster) SingletonServiceFactory.getBean(Cluster.class);

    @Test
    public void testServiceToUrl() {
        String s = cluster.serviceToUrl("http", "com.networknt.apib-1.0.0", null, null);
        Assert.assertTrue("http://localhost:7005".equals(s) || "http://localhost:7002".equals(s));
        s = cluster.serviceToUrl("http", "com.networknt.apib-1.0.0", null, null);
        Assert.assertTrue("http://localhost:7005".equals(s) || "http://localhost:7002".equals(s));
    }

    @Test
    public void testServices() {
        List<URI> l = cluster.services("http", "com.networknt.apib-1.0.0", null);
        Assert.assertEquals(2, l.size());
    }
}
