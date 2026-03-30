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

package com.networknt.portal.registry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

class PortalRegistryServiceTest {

    @Test
    void testToString() {
        PortalRegistryService service = new PortalRegistryService();
        service.setServiceId("com.networknt.apib-1.0.0");
        service.setName("apib");
        service.setProtocol("https");
        service.setAddress("127.0.0.1");
        service.setPort(7442);
        service.setTag("uat1");

        String s = service.toString();
        System.out.println("s = " + s);
        Assertions.assertEquals("{\"serviceId\":\"com.networknt.apib-1.0.0\",\"name\":\"apib\",\"tag\":\"uat1\",\"protocol\":\"https\",\"address\":\"127.0.0.1\",\"port\":7442,\"key\":\"com.networknt.apib-1.0.0|uat1\"}", s);
    }

    @Test
    void testUnifiedRegisterParamsUseEnvTagAddressAndVersion() {
        PortalRegistryService service = new PortalRegistryService();
        service.setServiceId("com.networknt.apib-1.0.0");
        service.setProtocol("https");
        service.setAddress("127.0.0.1");
        service.setPort(7442);
        service.setTag("uat1");
        service.setVersion("1.2.3");

        Map<String, Object> params = service.toRegisterParams("raw-jwt");
        Assertions.assertEquals("raw-jwt", params.get("jwt"));
        Assertions.assertEquals("uat1", params.get("envTag"));
        Assertions.assertEquals("127.0.0.1", params.get("address"));
        Assertions.assertFalse(params.containsKey("environment"));
    }

    @Test
    void testControllerRsRegisterParamsUseVersionAndJwtPayload() {
        PortalRegistryService service = new PortalRegistryService();
        service.setServiceId("com.networknt.apib-1.0.0");
        service.setProtocol("https");
        service.setAddress("127.0.0.1");
        service.setPort(7442);
        service.setTag("uat1");
        service.setVersion("1.2.3");

        Assertions.assertEquals("1.2.3", service.toRegisterParams("raw-jwt").get("version"));
        Assertions.assertEquals("raw-jwt", service.toRegisterParams("raw-jwt").get("jwt"));
        Assertions.assertEquals("uat1", service.toRegisterParams("raw-jwt").get("envTag"));
        Assertions.assertEquals("1.2.3", service.toRegisterParams("raw-jwt").get("version"));
    }
}
