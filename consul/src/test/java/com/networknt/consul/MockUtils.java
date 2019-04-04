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

import com.networknt.registry.URLImpl;
import com.networknt.registry.URLParamType;
import com.networknt.registry.URL;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhanglei28
 * @Description MockUtils
 */
public class MockUtils {
    //  mock service info
    private static String path = "mockService";
    private static String group = "mockService";
    private static String address = "127.0.0.1";
    private static String protocol = "http";

    public static ConsulService getMockService(int id) {

        ConsulService service = new ConsulService();
        service.setAddress(address);
        service.setId(ConsulUtils.convertServiceId(address, id, path));
        service.setName(ConsulUtils.convertGroupToServiceName(group));
        service.setPort(id);
        List<String> tags = new ArrayList<String>();
        tags.add(ConsulConstants.CONSUL_TAG_LIGHT_PROTOCOL + ":" + protocol);
        service.setTags(tags);

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
        //params.put(URLParamType.group.getName(), group);
        //params.put(URLParamType.protocol.getName(), protocol);
        URL url = new URLImpl(protocol, address, port, path, params);
        return url;
    }

}
