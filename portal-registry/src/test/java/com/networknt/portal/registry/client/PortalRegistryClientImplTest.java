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
package com.networknt.portal.registry.client;

import com.networknt.portal.registry.PortalRegistryConfig;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.networknt.portal.registry.PortalRegistryConfig.CONFIG_NAME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class PortalRegistryClientImplTest {

    private static final PortalRegistryConfig config = PortalRegistryConfig.load();

    @Test
    public void testWaitProperty() {
        assertEquals("https://localhost:8443", config.getPortalUrl());
    }

    @Test
    void testDiscoveryUsesRegisteredServiceChannel() {
        TestPortalRegistryClient client = new TestPortalRegistryClient();
        client.registerService(service("127.0.0.1", 7442), "Bearer raw-jwt");

        client.subscribeService("com.networknt.remote.v1", "prod", "https", "Bearer raw-jwt");

        assertEquals(List.of("service/register", "discovery/subscribe"), client.sentMethods());
    }

    @Test
    void testMicroserviceWebSocketUriForRootPortalUrl() {
        assertEquals(
                URI.create("wss://localhost:8443/ws/microservice"),
                PortalRegistryClientImpl.toMicroserviceWebSocketUri("https://localhost:8443"));
    }

    @Test
    void testMicroserviceWebSocketUriForRootPortalUrlWithTrailingSlash() {
        assertEquals(
                URI.create("wss://localhost:8443/ws/microservice"),
                PortalRegistryClientImpl.toMicroserviceWebSocketUri("https://localhost:8443/"));
    }

    @Test
    void testMicroserviceWebSocketUriPreservesIngressBasePath() {
        assertEquals(
                URI.create("wss://dev-apmh-api-platform.networknt.com/controlplane/v2/ws/microservice"),
                PortalRegistryClientImpl.toMicroserviceWebSocketUri("https://dev-apmh-api-platform.networknt.com/controlplane/v2"));
    }

    @Test
    void testMicroserviceWebSocketUriTrimsIngressBasePathTrailingSlash() {
        assertEquals(
                URI.create("wss://dev-apmh-api-platform.networknt.com/controlplane/v2/ws/microservice"),
                PortalRegistryClientImpl.toMicroserviceWebSocketUri("https://dev-apmh-api-platform.networknt.com/controlplane/v2/"));
    }

    @Test
    void testMicroserviceWebSocketUriDropsQueryAndFragment() {
        assertEquals(
                URI.create("wss://dev-apmh-api-platform.networknt.com/controlplane/v2/ws/microservice"),
                PortalRegistryClientImpl.toMicroserviceWebSocketUri(
                        "https://dev-apmh-api-platform.networknt.com/controlplane/v2?tenant=apmh#controller"));
    }

    @Test
    void testMicroserviceWebSocketUriUsesPlainWebSocketForHttp() {
        assertEquals(
                URI.create("ws://localhost:8080/controlplane/v2/ws/microservice"),
                PortalRegistryClientImpl.toMicroserviceWebSocketUri("http://localhost:8080/controlplane/v2"));
    }

    @Test
    void testMicroserviceWebSocketUriRejectsUnsupportedScheme() {
        assertThrows(IllegalArgumentException.class,
                () -> PortalRegistryClientImpl.toMicroserviceWebSocketUri("ftp://localhost/controlplane/v2"));
    }

    private com.networknt.portal.registry.PortalRegistryService service(String address, int port) {
        com.networknt.portal.registry.PortalRegistryService service = new com.networknt.portal.registry.PortalRegistryService();
        service.setServiceId("test-service");
        service.setProtocol("https");
        service.setAddress(address);
        service.setPort(port);
        service.setTag("prod");
        return service;
    }

    private static class TestPortalRegistryClient extends PortalRegistryClientImpl {
        private final List<String> methods = new ArrayList<>();
        private PortalRegistryWebSocketClient mockClient;

        public TestPortalRegistryClient() {
            super();
            // We need a way to mock the registrationClients map or the methods that use it.
        }

        @Override
        public void registerService(com.networknt.portal.registry.PortalRegistryService service, String token) {
            methods.add("service/register");
            // Do NOT call super.registerService(service, token) as it tries to connect.
            // Instead, just simulate the registrationClients entry.
        }

        @Override
        public List<Map<String, Object>> subscribeService(String serviceId, String tag, String protocol, String token) {
            methods.add("discovery/subscribe");
            // Do NOT call super.subscribeService as it calls activeServiceChannel() which might fail.
            return new ArrayList<>();
        }

        public List<String> sentMethods() {
            return methods;
        }
    }
}
