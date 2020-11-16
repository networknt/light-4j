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

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

import static com.networknt.portal.registry.PortalRegistryConfig.CONFIG_NAME;

public class PortalRegistryServiceTest {
    static PortalRegistryConfig config = (PortalRegistryConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, PortalRegistryConfig.class);
    @Test
    public void testToString() {
        PortalRegistryService service = new PortalRegistryService();
        service.setServiceId("com.networknt.apib-1.0.0");
        service.setName("apib");
        service.setAddress("127.0.0.1");
        service.setPort(7442);
        service.setTag("uat1");

        String s = service.toString();
        System.out.println("s = " + s);
        if(config.httpCheck) {
            Assert.assertEquals("{\"serviceId\":\"com.networknt.apib-1.0.0\",\"name\":\"apib\",\"tag\":\"uat1\",\"address\":\"127.0.0.1\",\"port\":7442,\"check\":{\"id\":\"com.networknt.apib-1.0.0|uat1:127.0.0.1:7442\",\"deregisterCriticalServiceAfter\":120000,\"http\":\"https://127.0.0.1:7442/health/com.networknt.apib-1.0.0\",\"tlsSkipVerify\":true,\"interval\":10000}}", s);
        } else {
            Assert.assertEquals("{\"serviceId\":\"com.networknt.apib-1.0.0\",\"name\":\"apib\",\"tag\":\"uat1\",\"address\":\"127.0.0.1\",\"port\":7442,\"check\":{\"id\":\"com.networknt.apib-1.0.0|uat1:127.0.0.1:7442\",\"deregisterCriticalServiceAfter\":120000,\"interval\":10000}}", s);
        }
    }
}
