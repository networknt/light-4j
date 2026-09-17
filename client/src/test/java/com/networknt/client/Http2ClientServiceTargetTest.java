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

package com.networknt.client;

import io.undertow.client.ClientRequest;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;

/**
 * Test the target resolved from the service discovery is applied to the request before it is sent. It covers the
 * base path of a path based k8s ingress and the Host header an ingress or a virtual host routes on.
 */
class Http2ClientServiceTargetTest {
    @Test
    void testBasePathAndHostHeaderAreApplied() {
        ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/v1/pets");

        Http2Client.applyServiceTarget(URI.create("https://api.example.com:443/namespace1/service1"), request);

        Assertions.assertEquals("/namespace1/service1/v1/pets", request.getPath());
        Assertions.assertEquals("api.example.com", request.getRequestHeaders().getFirst(Headers.HOST));
    }

    @Test
    void testHostHeaderKeepsANonDefaultPort() {
        ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/v1/pets");

        Http2Client.applyServiceTarget(URI.create("https://api.example.com:8443"), request);

        Assertions.assertEquals("/v1/pets", request.getPath());
        Assertions.assertEquals("api.example.com:8443", request.getRequestHeaders().getFirst(Headers.HOST));
    }

    @Test
    void testExplicitHostHeaderIsPreserved() {
        ClientRequest request = new ClientRequest().setMethod(Methods.GET).setPath("/v1/pets");
        request.getRequestHeaders().put(Headers.HOST, "petstore.example.com");

        Http2Client.applyServiceTarget(URI.create("https://api.example.com:443/namespace1/service1"), request);

        Assertions.assertEquals("/namespace1/service1/v1/pets", request.getPath());
        Assertions.assertEquals("petstore.example.com", request.getRequestHeaders().getFirst(Headers.HOST));
    }
}
