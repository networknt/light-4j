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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

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
        Assertions.assertTrue("http://localhost:7005".equals(s) || "http://localhost:7002".equals(s));
        s = cluster.serviceToUrl("http", "com.networknt.apib-1.0.0", null, null);
        Assertions.assertTrue("http://localhost:7005".equals(s) || "http://localhost:7002".equals(s));
    }

    @Test
    public void testServiceToUrlWithEnvironment() {
        String s = cluster.serviceToUrl("https", "com.networknt.portal.command-1.0.0", "0000", null);
        System.out.println(s);
        Assertions.assertTrue("https://localhost:8440".equals(s));
        s = cluster.serviceToUrl("https", "com.networknt.portal.command-1.0.0", "0001", null);
        System.out.println(s);
        Assertions.assertTrue("https://localhost:8441".equals(s));
        s = cluster.serviceToUrl("https", "com.networknt.portal.command-1.0.0", "0002", null);
        System.out.println(s);
        Assertions.assertTrue("https://localhost:8442".equals(s));

    }

    @Test
    public void testServiceToSingleUrlWithEnv() {
        String s = cluster.serviceToUrl("https", "com.networknt.chainwriter-1.0.0", "0000", null);
        Assertions.assertTrue("https://localhost:8444".equals(s));
    }

    @Test
    public void testServiceToUrlWithBasePath() {
        String s = cluster.serviceToUrl("https", "com.networknt.ingress-1.0.0", null, null);
        Assertions.assertEquals("https://api.example.com:443/namespace1/service1", s);
    }

    @Test
    public void testServicesWithBasePath() {
        List<URI> l = cluster.services("https", "com.networknt.ingress-1.0.0", null);
        Assertions.assertEquals(1, l.size());
        Assertions.assertEquals("/namespace1/service1", l.get(0).getPath());
    }

    @Test
    public void testEncodedBasePathIsNotEncodedTwice() {
        String s = cluster.serviceToUrl("https", "com.networknt.encoded-1.0.0", null, null);
        Assertions.assertEquals("https://api.example.com:443/ns/a%20b", s);

        List<URI> l = cluster.services("https", "com.networknt.encoded-1.0.0", null);
        Assertions.assertEquals(1, l.size());
        Assertions.assertEquals("/ns/a%20b", l.get(0).getRawPath());
        Assertions.assertEquals("/ns/a b", l.get(0).getPath());
        Assertions.assertEquals("/ns/a%20b/v1/pets", Cluster.prependBasePath(l.get(0), "/v1/pets"));
    }

    @Test
    public void testPrependBasePath() throws Exception {
        URI uri = new URI("https://api.example.com:443/namespace1/service1");
        Assertions.assertEquals("/namespace1/service1/v1/pets", Cluster.prependBasePath(uri, "/v1/pets"));
        Assertions.assertEquals("/v1/pets", Cluster.prependBasePath(new URI("https://api.example.com:443"), "/v1/pets"));
        Assertions.assertEquals("/v1/pets", Cluster.prependBasePath(new URI("https://api.example.com:443/"), "/v1/pets"));
    }

    @Test
    public void testHostHeader() {
        Assertions.assertEquals("api.example.com", Cluster.hostHeader(URI.create("https://api.example.com:443/ns/svc")));
        Assertions.assertEquals("api.example.com", Cluster.hostHeader(URI.create("http://api.example.com:80")));
        Assertions.assertEquals("api.example.com:8443", Cluster.hostHeader(URI.create("https://api.example.com:8443")));
        Assertions.assertEquals("api.example.com", Cluster.hostHeader(URI.create("https://api.example.com")));
        Assertions.assertNull(Cluster.hostHeader(null));
    }

    @Test
    public void testServices() {
        List<URI> l = cluster.services("http", "com.networknt.apib-1.0.0", null);
        Assertions.assertEquals(2, l.size());
    }
}
