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

import com.networknt.utility.Constants;

import java.net.URI;
import java.util.List;

/**
 * Cluster interface is used to lookup a service instance by protocol, service id
 * and requestKey if necessary. Under the hood, it calls load balance to pick up
 * an instance from multiple instances retrieved from client side service discovery.
 *
 * Created by stevehu on 2017-01-27.
 */
public interface Cluster {
    /**
     * give a service name and return a url with http or https url
     * the result is has been gone through the load balance with request key
     *
     * requestKey is used to control the behavior of load balance except
     * round robin and local first which this value is null. For consistent hash
     * load balance, normally client_id or user_id from JWT token should be passed
     * in to route the same client to the same server all the time or the same user
     * to the same server all the time
     *
     * @param protocol either http or https
     * @param serviceId unique service identifier
     * @param tag an environment tag use along with serviceId for discovery
     * @param requestKey load balancer key
     * @return String url
     */
    String serviceToUrl(String protocol, String serviceId, String tag, String requestKey);

    /**
     * give a service name and return a list of URI object that represent the services
     * returned from the discovery lookup. It gives the client an opportunity to load
     * balance by itself. The main usage is for light-router to balance between muliple
     * instances of downstream services.
     *
     * @param protocol either http or https
     * @param serviceId unique service identifier
     * @param tag an environment tag use along with serviceId for discovery
     * @return List of URI objects
     */
    List<URI> services(String protocol, String serviceId, String tag);

    /**
     * The url returned from the serviceToUrl and the URI returned from the services might contain a base path
     * when the target service is deployed behind a path based k8s ingress. For example, the url might be
     * https://api.example.com:443/namespace1/service1 in which the /namespace1/service1 is used by the ingress
     * to route the request to the right pod and stripped before the request reaches the pod. As the path of the
     * request is built by the caller, the base path must be prepended to it before the request is sent.
     *
     * @param uri the URI created from the serviceToUrl or returned from the services
     * @param path the path of the request built by the caller
     * @return the path with the base path of the target service prepended
     */
    static String prependBasePath(URI uri, String path) {
        if (uri == null) {
            return path;
        }
        String basePath = uri.getRawPath();
        if (basePath == null || basePath.isBlank() || Constants.PATH_SEPARATOR.equals(basePath)) {
            return path;
        }
        while (basePath.endsWith(Constants.PATH_SEPARATOR)) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        if (!basePath.startsWith(Constants.PATH_SEPARATOR)) {
            basePath = Constants.PATH_SEPARATOR + basePath;
        }
        if (path == null || path.isEmpty()) {
            return basePath;
        }
        return path.startsWith(Constants.PATH_SEPARATOR) ? basePath + path : basePath + Constants.PATH_SEPARATOR + path;
    }

    /**
     * Build the Host header for a request to the target resolved by the serviceToUrl or the services. An ingress or
     * a virtual host routes on the Host header, so the header must reflect the host of the target instead of the
     * default localhost. The port is only included when it is not the default port of the protocol.
     *
     * @param uri the URI created from the serviceToUrl or returned from the services
     * @return the value of the Host header or null if the URI doesn't have a host
     */
    static String hostHeader(URI uri) {
        if (uri == null || uri.getHost() == null) {
            return null;
        }
        String host = uri.getHost();
        int port = uri.getPort();
        String scheme = uri.getScheme();
        boolean defaultPort = port == -1
                || (port == 443 && "https".equalsIgnoreCase(scheme))
                || (port == 80 && "http".equalsIgnoreCase(scheme));
        return defaultPort ? host : host + ":" + port;
    }
}
