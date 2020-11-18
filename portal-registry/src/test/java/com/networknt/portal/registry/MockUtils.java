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

import com.networknt.registry.URLImpl;
import com.networknt.registry.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MockUtils {
    //  mock service info
    private static String path = "com.networknt.mockService.v1";
    private static String group = "mockService";
    private static String address = "127.0.0.1";
    private static String protocol = "http";
    private static String tag = "uat1";

    public static PortalRegistryService getMockService(int port) {
        PortalRegistryService service = new PortalRegistryService();
        service.setAddress(address);
        service.setServiceId(path);
        service.setName(group);
        service.setProtocol(protocol);
        service.setPort(port);
        service.setTag(tag);
        return service;
    }

    /**
     * get mock url, use it to query mock service
     *
     * @return URL a URL object
     */
    public static URL getMockUrl(int port) {
        return getMockUrl(address, port);
    }

    public static URL getMockUrl(String address, int port) {
        Map<String, String> params = new HashMap<>();
        URL url = new URLImpl(protocol, address, port, path, params);
        return url;
    }
}
