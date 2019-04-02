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


}
