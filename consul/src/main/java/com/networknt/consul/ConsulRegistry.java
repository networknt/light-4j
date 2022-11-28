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

package com.networknt.consul;

import com.networknt.client.Http2Client;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;
import com.networknt.consul.client.ConsulClient;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.URL;
import com.networknt.registry.URLParamType;
import com.networknt.registry.support.command.CommandFailbackRegistry;
import com.networknt.registry.support.command.CommandServiceManager;
import com.networknt.registry.support.command.ServiceListener;
import com.networknt.utility.Constants;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ConsulRegistry extends CommandFailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistry.class);
    private ConsulClient client;
    private ConsulHeartbeatManager heartbeatManager;
    private int lookupInterval;

    // service local cache. key: serviceName, value: <service url list>
    private ConcurrentHashMap<String, List<URL>> serviceCache = new ConcurrentHashMap<String, List<URL>>();

    // record lookup service thread, ensure each serviceName start only one thread, <serviceName, lastConsulIndexId>
    private ConcurrentHashMap<String, Long> lookupServices = new ConcurrentHashMap<String, Long>();

    // TODO: 2016/6/17 clientUrl support multiple listener
    // record subscribers service callback listeners, listener was called when corresponding service changes
    private ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceListener>> serviceListeners = new ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceListener>>();
    private ThreadPoolExecutor notifyExecutor;

    public ConsulRegistry(URL url, ConsulClient client) {
        super(url);
        this.client = client;
        if(getConsulConfig().ttlCheck) {
            heartbeatManager = new ConsulHeartbeatManager(client, getConsulToken());
            heartbeatManager.start();
        }
        lookupInterval = getUrl().getIntParameter(URLParamType.registrySessionTimeout.getName(), ConsulConstants.DEFAULT_LOOKUP_INTERVAL);

        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(20000);
        notifyExecutor = new ThreadPoolExecutor(10, 30, 30 * 1000, TimeUnit.MILLISECONDS, workQueue);
        logger.info("ConsulRegistry init finish.");
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceListener>> getServiceListeners() {
        return serviceListeners;
    }

    @Override
    protected void doRegister(URL url) {
        ConsulService service = ConsulUtils.buildService(url);
        client.registerService(service, getConsulToken());
        if(getConsulConfig().ttlCheck) heartbeatManager.addHeartbeatServcieId(service.getId());
    }

    @Override
    protected void doUnregister(URL url) {
        ConsulService service = ConsulUtils.buildService(url);
        client.unregisterService(service.getId(), getConsulToken());
        if(getConsulConfig().ttlCheck) heartbeatManager.removeHeartbeatServiceId(service.getId());
    }

    @Override
    protected void doAvailable(URL url) {
        if (url == null) {
            if(getConsulConfig().ttlCheck) heartbeatManager.setHeartbeatOpen(true);
        } else {
            throw new UnsupportedOperationException("Command consul registry not support available by urls yet");
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        if (url == null) {
            if(getConsulConfig().ttlCheck) heartbeatManager.setHeartbeatOpen(false);
        } else {
            throw new UnsupportedOperationException("Command consul registry not support unavailable by urls yet");
        }
    }

    @Override
    protected void subscribeService(URL url, ServiceListener serviceListener) {
        addServiceListener(url, serviceListener);
        startListenerThreadIfNewService(url);
    }

    /**
     * Override the method in <code>com.networknt.registry.support.command.CommandFailbackRegistry</code>
     * to skip calling the <code>com.networknt.registry.support.command.CommandFailbackRegistry#doDiscover()</code> and
     * <code>com.networknt.registry.support.command.CommandFailbackRegistry#notify()</code>
     * @param url The subscribed service URL
     * @param listener  The listener to be notified when service registration changed.
     */
    @Override
    protected void doSubscribe(URL url, final NotifyListener listener) {
        if(logger.isInfoEnabled()) logger.info("CommandFailbackRegistry subscribe. url: " + url.toSimpleString());
        URL urlCopy = url.createCopy();
        CommandServiceManager manager = getCommandServiceManager(urlCopy);
        manager.addNotifyListener(listener);

        subscribeService(urlCopy, manager);
    }

    /**
     * if new service registered, start a new lookup thread
     * each serviceName start a lookup thread to discover service
     *
     * @param url
     */
    private void startListenerThreadIfNewService(URL url)
    {
        String serviceName = url.getPath();

        // Do NOT start a listener thread if serviceName is blank
        if(StringUtils.isBlank(serviceName))
            return;

        String protocol = url.getProtocol();
        if (!lookupServices.containsKey(serviceName)) {
            Long value = lookupServices.putIfAbsent(serviceName, 0L);
            if (value == null) {
                ServiceLookupThread lookupThread = new ServiceLookupThread(protocol, serviceName);
                lookupThread.setDaemon(true);
                lookupThread.start();
            }
        }
    }

    private void addServiceListener(URL url, ServiceListener serviceListener) {
        String service = ConsulUtils.getUrlClusterInfo(url);
        ConcurrentHashMap<URL, ServiceListener> map = serviceListeners.get(service);
        if (map == null) {
            serviceListeners.putIfAbsent(service, new ConcurrentHashMap<URL, ServiceListener>());
            map = serviceListeners.get(service);
        }
        synchronized (map) {
            map.put(url, serviceListener);
        }
    }

    @Override
    protected void unsubscribeService(URL url, ServiceListener listener) {
        ConcurrentHashMap<URL, ServiceListener> listeners = serviceListeners.get(ConsulUtils.getUrlClusterInfo(url));
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(url);
            }
        }
    }

    @Override
    protected List<URL> discoverService(URL url) {
        String serviceName = url.getPath();
        String tag = url.getParameter(Constants.TAG_ENVIRONMENT);
        String protocol = url.getProtocol();
        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " serviceName = " + serviceName + " tag = " + tag);
        List<URL> urls = serviceCache.get(serviceName);
        if (urls == null || urls .isEmpty()) {
            synchronized (serviceName.intern()) {
                urls = serviceCache.get(serviceName);
                if (urls == null || urls.isEmpty()) {
                    ConcurrentHashMap<String, List<URL>> serviceUrls = lookupServiceUpdate(protocol, serviceName, false);
                    updateServiceCache(serviceName, serviceUrls, false);
                    urls = serviceCache.get(serviceName);
                }
            }
        }
        return urls;
    }

    private ConcurrentHashMap<String, List<URL>> lookupServiceUpdate(String protocol, String serviceName) {
        return lookupServiceUpdate(protocol, serviceName, true);
    }

    /***
     *
     * @param   protocol
     * @param   serviceName
     * @param   isBlockQuery
     * @return  serviceUrls.size() == 0 (e.g.: Map has no k/v pairs),
     *          if:
     *              - Consul index was stale, or
     *              - Consul reported no updates since last query, or
     *              - Connection to Consul failed
     *
     *              This result indicates to updateServiceCache() to leave local registry cache unchanged
     *
     *          serviceUrls.size() > 0 &&
     *          serviceUrls.get(serviceName) != null &&
     *          serviceUrls.get(serviceName).size() == number of IPs registered for serviceName in Consul,
     *          if:
     *              - the IP set registered for serviceName has changed
     *
     *              This result indicates to updateServiceCache() to update the local registry cache
     */
    private ConcurrentHashMap<String, List<URL>> lookupServiceUpdate(String protocol, String serviceName, boolean isBlockQuery)
    {
        // get Consul index if blocking query
        Long lastConsulIndexId = 0L;
        if (isBlockQuery) {
            lastConsulIndexId = lookupServices.get(serviceName) == null ? 0L : lookupServices.get(serviceName);
        }
        logger.debug("serviceName = {} lastConsulIndexId = {}", serviceName, lastConsulIndexId);

        // response should be null iff there was an error connecting to Consul
        ConsulResponse<List<ConsulService>> response = lookupConsulService(serviceName, lastConsulIndexId);

        if(logger.isTraceEnabled()) {
            try { logger.trace("response = " + Config.getInstance().getMapper().writeValueAsString(response));
            } catch (Exception e) {}
        }

        // initialize serviceUrls such that serviceUrls.size() == 0, this indicate to updateServiceCache() to leave
        // cache unchanged
        ConcurrentHashMap<String, List<URL>> serviceUrls = new ConcurrentHashMap<>();

        if (response != null)
        {
            List<ConsulService> services = response.getValue();
            if(logger.isDebugEnabled())
                try { logger.debug("Consul-registered services = " + Config.getInstance().getMapper().writeValueAsString(services));
                } catch (Exception e) {}

            /***
             * Since response != null, and services = response.getValue(), we know (from the specification for
             * ConsulClient.lookupHealthService()) that:
             *
             *      - Consul connection was successful, and
             *      - services != null, and
             *      - services.size() == number of IPs registered for serviceName in Consul
             */
            // if (services != null && !services.isEmpty() && response.getConsulIndex() > lastConsulIndexId)
            if (response.getConsulIndex() > lastConsulIndexId)
            {
                logger.info("Got updated urls from Consul: {} instances of service {} found", services.size(), serviceName);

                // - Update has occurred: Ensure that the serviceUrls Map has at least one (possibly empty List)
                //   entry for the serviceName key.
                //   This will ensure that updateServiceCache() will do an update.
                if(services.size() == 0)
                    serviceUrls.put(serviceName, new ArrayList<>());

                for (ConsulService service : services) {
                    try {

                        URL url = ConsulUtils.buildUrl(protocol, service);
                        List<URL> urlList = serviceUrls.get(serviceName);

                        if (urlList == null) {
                            urlList = new ArrayList<>();
                            serviceUrls.put(serviceName, urlList);
                        }

                        if(logger.isTraceEnabled())
                            logger.trace("Consul lookupServiceUpdate url = " + url);

                        urlList.add(url);

                    } catch (Exception e) {
                        logger.error("Failed to convert Consul service to url! service: " + service, e);
                    }
                }

                lookupServices.put(serviceName, response.getConsulIndex());
                logger.info("Consul index put into lookupServices for service: {}, index={}", serviceName, response.getConsulIndex());

                return serviceUrls;

            } else if (response.getConsulIndex() < lastConsulIndexId) {
                logger.info("Consul returned stale index: Index reset to 0 for service {} - Consul response index < last Consul index: {} < {}", serviceName, response.getConsulIndex(), lastConsulIndexId);

                // force a fresh list of services from Consul
                lookupServices.put(serviceName, 0L);

                // Indicate to updateServiceCache() to leave cache unchanged for now:
                // - serviceUrls.isEmpty() == true && serviceUrls.get(serviceName) != null && serviceUrls.get(serviceName).size() == 0
            } else {
                logger.info("Consul returned no service updates: No need to update local Consul discovery cache for service {}, lastIndex={}", serviceName, lastConsulIndexId);

                // Indicate to updateServiceCache() to leave cache unchanged for now:
                // - serviceUrls.isEmpty() == true && serviceUrls.get(serviceName) != null && serviceUrls.get(serviceName).size() == 0
            }
        } else {
            logger.info("Connection to Consul failed for service {} - Local service cache may be out of date", serviceName);

            // Indicate to updateServiceCache() to leave cache unchanged for now:
            // - serviceUrls.isEmpty() == true && serviceUrls.get(serviceName) != null && serviceUrls.get(serviceName).size() == 0
        }

        return serviceUrls;
    }

    /**
     * directly fetch consul service data.
     *
     * @param serviceName
     * @return ConsulResponse or null
     */
    private ConsulResponse<List<ConsulService>> lookupConsulService(String serviceName, Long lastConsulIndexId) {
        ConsulResponse<List<ConsulService>> response = client.lookupHealthService(serviceName, null, lastConsulIndexId, getConsulToken());
        return response;
    }

    /**
     * update service cache of the serviceName.
     * update local cache when service list changed,
     * if need notify, notify service
     *
     *  Q: Why do we not update if serviceUrls.isEmpty() == true ?
     *  A: serviceUrls.isEmpty() == true indicates that the local cache should not be changed.
     *     Leaving the local cache unchanged can allow consumer requests to continue to be proxied
     *     even in the event that the connection to Consul temporarily fails.
     *     This provides time to re-establish the connection with Consul while not disrupting
     *     consumer requests (as long as the services the consumer is targetting have not changed
     *     their IP addresses during the time the Consul connection is offline)
     *
     *  Q: How we know when cache needs to be emptied?
     *  A: serviceUrls.isEmpty() == false              // - indicates connection to Consul is OK
     *     serviceUrls.get(serviceName) != null &&     // - indicates that Consul has reported that serviceName
     *     serviceUrls.get(serviceName).size() == 0    //   has no IPs registered
     *
     * @param serviceName
     * @param serviceUrls   Leave cache as-is and do not notify if serviceUrls == null || serviceUrls.isEmpty()
     * @param needNotify
     */
    private void updateServiceCache(String serviceName, ConcurrentHashMap<String, List<URL>> serviceUrls, boolean needNotify)
    {
        if (serviceUrls != null && !serviceUrls.isEmpty())
        {
            List<URL> cachedUrls = serviceCache.get(serviceName);
            List<URL> newUrls = serviceUrls.get(serviceName);
            try {
                logger.trace("Consul service URLs = {}", Config.getInstance().getMapper().writeValueAsString(serviceUrls));
            } catch(Exception e) {
            }

            boolean change = true;
            if (ConsulUtils.isSame(newUrls, cachedUrls)) {
                change = false;
            } else {
                serviceCache.put(serviceName, newUrls);
            }

            if (change && needNotify) {
                notifyExecutor.execute(new NotifyService(serviceName, newUrls));
                logger.info("light service notify-service: " + serviceName);
                StringBuilder sb = new StringBuilder();
                for (URL url : newUrls) {
                    sb.append(url.getUri()).append(";");
                }
                logger.info("Consul notify URLs:" + sb.toString());
            }
        }
    }

    private class ServiceLookupThread extends Thread {
       private String protocol;
       private String serviceName;

        public ServiceLookupThread(String protocol, String serviceName) {
            this.protocol = protocol;
            this.serviceName = serviceName;
        }

        // TODO: The MAIN CONSUL LOOP
        @Override
        public void run() {
            logger.info("Start Consul lookupServiceUpdate thread - Lookup interval: {}ms, service {}", lookupInterval, serviceName);
            while (true) {
                try {
                    logger.info("Consul lookupServiceUpdate Thread - SLEEP: Start to sleep {}ms for service {}", lookupInterval, serviceName);
                    sleep(lookupInterval);

                    logger.info("Consul lookupServiceUpdate Thread - WAKE UP: Woke up from sleep for lookupServiceUpdate for service {}", serviceName);
                    ConcurrentHashMap<String, List<URL>> serviceUrls = lookupServiceUpdate(protocol, serviceName);

                    if(serviceUrls == null || serviceUrls.size() == 0)
                        logger.debug("No service URL updates from Consul lookupServiceUpdate for service {}", serviceName);
                    else
                        logger.debug("Got service URLs from Consul lookupServiceUpdate: {} service URLs found for service {} ({})",
                                serviceUrls.getOrDefault(serviceName, Collections.emptyList()).size(), serviceName, protocol);

                    // TODO: DO NOT update local service cache if serviceUrls == null
                    // TODO: BUT *DO* update local service cache if serviceUrls != null (even if empty list)
                    updateServiceCache(serviceName, serviceUrls, true);

                    logger.info("Local Consul service cache updated with service URLs from lookupServiceUpdate for {}", serviceName);

                } catch (Throwable e) {
                    logger.error("Consul lookupServiceUpdate thread failed!", e);
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
    }

    private class NotifyService implements Runnable {
        private String service;
        private List<URL> urls;

        public NotifyService(String service, List<URL> urls) {
            this.service = service;
            this.urls = urls;
        }

        @Override
        public void run() {
            ConcurrentHashMap<URL, ServiceListener> listeners = serviceListeners.get(service);
            if (listeners != null) {
                synchronized (listeners) {
                    for (Map.Entry<URL, ServiceListener> entry : listeners.entrySet()) {
                        ServiceListener serviceListener = entry.getValue();
                        serviceListener.notifyService(entry.getKey(), getUrl(), urls);
                    }
                }
            } else {
                logger.debug("need not notify service:" + service);
            }
        }
    }

    private ConsulConfig getConsulConfig(){
        return (ConsulConfig)Config.getInstance().getJsonObjectConfig(ConsulConstants.CONFIG_NAME, ConsulConfig.class);
    }

    private String getConsulToken(){
        Map<String, Object> secret = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET);
        String token = secret == null? null : (String)secret.get(SecretConstants.CONSUL_TOKEN);
        return token;
    }

}
