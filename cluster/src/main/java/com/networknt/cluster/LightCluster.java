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

import com.networknt.balance.LoadBalance;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.registry.URLImpl;
import com.networknt.service.SingletonServiceFactory;
import com.networknt.utility.ConcurrentHashSet;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * This is the only concrete implementation of cluster interface. It basically integrates
 * service discovery, service registry and load balance together to provide a common way
 * to convert a protocal, service id and request key to a url that can be addressed and
 * invoked.
 *
 * Created by stevehu on 2017-01-27.
 */
public class LightCluster implements Cluster {
    private static Logger logger = LoggerFactory.getLogger(LightCluster.class);
    private static Registry registry = SingletonServiceFactory.getBean(Registry.class);
    private static LoadBalance loadBalance = SingletonServiceFactory.getBean(LoadBalance.class);
    private static Set<URL> subscribedSet = new ConcurrentHashSet<>();
    private static Map<String, List<URL>> serviceMap = new ConcurrentHashMap<>();

    public LightCluster() {
        if(logger.isInfoEnabled()) logger.info("A LightCluster instance is started");
    }

    /**
     * Implement serviceToUrl with client side service discovery.
     *
     * @param protocol String
     * @param serviceId String
     * @param requestKey String
     * @return String
     */
    @Override
    public String serviceToUrl(String protocol, String serviceId, String tag, String requestKey) {
        URL url = loadBalance.select(discovery(protocol, serviceId, tag), requestKey);
        if(logger.isDebugEnabled()) logger.debug("final url after load balance = " + url);
        // construct a url in string
        return protocol + "://" + url.getHost() + ":" + url.getPort();
    }

    /**
     *
     * @param protocol either http or https
     * @param serviceId unique service identifier
     * @param tag an environment tag use along with serviceId for discovery
     * @return List of URI objects
     */
    @Override
    public List<URI> services(String protocol, String serviceId, String tag) {
        // transform to a list of URIs
        return discovery(protocol, serviceId, tag).stream()
                .map(this::toUri)
                .collect(Collectors.toList());
    }

    private List<URL> discovery(String protocol, String serviceId, String tag) {
        if(logger.isDebugEnabled()) logger.debug("protocol = " + protocol + " serviceId = " + serviceId);
        // lookup in serviceMap first, if not there, then subscribe and discover.
        List<URL> urls = serviceMap.get(serviceId);
        if(logger.isDebugEnabled()) logger.debug("cached serviceId " + serviceId + " urls = " + urls);
        if(urls == null) {
            URL subscribeUrl = URLImpl.valueOf(protocol + "://localhost/" + serviceId);
            if(tag != null) {
                subscribeUrl.addParameter(Constants.TAG_ENVIRONMENT, tag);
            }
            if(logger.isDebugEnabled()) logger.debug("subscribeUrl = " + subscribeUrl);
            // you only need to subscribe once.
            if(!subscribedSet.contains(subscribeUrl)) {
                registry.subscribe(subscribeUrl, new ClusterNotifyListener());
                subscribedSet.add(subscribeUrl);
            }
            urls = registry.discover(subscribeUrl);
            if(logger.isDebugEnabled()) logger.debug("discovered urls = " + urls);
        }
        return urls;
    }

    private URI toUri(URL url) {
        URI uri = null;
        try {
            uri = new URI(url.getProtocol(), null, url.getHost(), url.getPort(), null, null, null);
        } catch (URISyntaxException e) {
            logger.error("URISyntaxExcpetion", e);
        }
        return uri;
    }

    static class ClusterNotifyListener implements NotifyListener {
        @Override
        public void notify(URL registryUrl, List<URL> urls) {
            if(logger.isDebugEnabled()) logger.debug("notify is called in ClusterNotifyListener registryUrl = " + registryUrl + " urls = " + urls);
            if(urls != null && urls.size() > 0) serviceMap.put(urls.get(0).getPath(), urls);
        }
    }
}
