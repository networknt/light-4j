/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.networknt.portal.registry;

import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.registry.URLParamType;
import com.networknt.utility.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PortalRegistryUtils {

    /**
     * Check if two lists have the same urls.
     *
     * @param urls1 first url list
     * @param urls2 second url list
     * @return boolean true when they are the same
     */
    public static boolean isSame(List<URL> urls1, List<URL> urls2) {
        if(urls1 == null && urls2 == null) {
            return true;
        }
        if (urls1 == null || urls2 == null) {
            return false;
        }
        if (urls1.size() != urls2.size()) {
            return false;
        }
        return urls1.containsAll(urls2);
    }

    /**
     * build consul service from url
     *
     * @param url a URL object
     * @return service PortalRegistryService
     */
    public static PortalRegistryService buildService(URL url) {
        PortalRegistryService service = new PortalRegistryService();
        service.setAddress(url.getHost());
        service.setServiceId(convertPortalRegistrySerivceId(url));
        service.setName(url.getPath());
        service.setPort(url.getPort());
        String env = url.getParameter(Constants.TAG_ENVIRONMENT);
        if(env != null) service.setTag(env);
        return service;
    }

    /**
     * build url from service
     * @param protocol the protocol of the service
     * @param service PortalRegistryService
     * @return URL object
     */
    public static URL buildUrl(String protocol, PortalRegistryService service) {
        URL url = null;
        if (url == null) {
            Map<String, String> params = new HashMap<>();
            if (service.getTag() != null) {
                params.put(URLParamType.environment.getName(), service.getTag());
            }
            url = new URLImpl(protocol, service.getAddress(), service.getPort(), service.getServiceId(), params);
        }
        return url;
    }

    /**
     * get cluster info from url, cluster info (protocol, path)
     *
     * @param url a URL object
     * @return String url cluster info
     */
    public static String getUrlClusterInfo(URL url) {
        return url.getPath();
    }

    /**
     * convert url to consul service id. serviceid includes ip＋port＋service
     *
     * @param url a URL object
     * @return service id
     */
    public static String convertPortalRegistrySerivceId(URL url) {
        return url.getPath();
    }

    /**
     * get path of url from service id in consul
     *
     * @param serviceId service id
     * @return path
     */
    public static String getPathFromServiceId(String serviceId) {
        return serviceId.substring(serviceId.indexOf(":") + 1, serviceId.lastIndexOf(":"));
    }
}
