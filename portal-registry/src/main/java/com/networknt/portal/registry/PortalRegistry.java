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

import com.networknt.config.Config;
import com.networknt.portal.registry.client.PortalRegistryClient;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.URL;
import com.networknt.registry.URLParamType;
import com.networknt.registry.support.command.CommandFailbackRegistry;
import com.networknt.registry.support.command.CommandServiceManager;
import com.networknt.registry.support.command.ServiceListener;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class PortalRegistry extends CommandFailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PortalRegistry.class);
    private static final String CONFIG_PROPERTY_MISSING = "ERR10057";

    private PortalRegistryClient client;
    private PortalRegistryHeartbeatManager heartbeatManager;
    private int lookupInterval;

    // service local cache. key: serviceName, value: <service url list>
    private ConcurrentHashMap<String, List<URL>> serviceCache = new ConcurrentHashMap<String, List<URL>>();

    // record lookup service thread, ensure each serviceName start only one thread, <serviceName, lastConsulIndexId>
    private ConcurrentHashMap<String, Long> lookupServices = new ConcurrentHashMap<String, Long>();

    // TODO: 2016/6/17 clientUrl support multiple listener
    // record subscribers service callback listeners, listener was called when corresponding service changes
    private ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceListener>> serviceListeners = new ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceListener>>();
    private ThreadPoolExecutor notifyExecutor;

    public PortalRegistry(URL url, PortalRegistryClient client) {
        super(url);
        this.client = client;
        if(getPortalRegistryConfig().ttlCheck) {
            heartbeatManager = new PortalRegistryHeartbeatManager(client, getPortalToken());
            heartbeatManager.start();
        }
        lookupInterval = getUrl().getIntParameter(URLParamType.registrySessionTimeout.getName(), PortalRegistryConstants.DEFAULT_LOOKUP_INTERVAL);

        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(20000);
        notifyExecutor = new ThreadPoolExecutor(10, 30, 30 * 1000, TimeUnit.MILLISECONDS, workQueue);
        logger.info("ConsulRegistry init finish.");
    }

    public ConcurrentHashMap<String, ConcurrentHashMap<URL, ServiceListener>> getServiceListeners() {
        return serviceListeners;
    }

    @Override
    protected void doRegister(URL url) {
        PortalRegistryService service = PortalRegistryUtils.buildService(url);
        client.registerService(service, getPortalToken());
        if(getPortalRegistryConfig().ttlCheck) heartbeatManager.addHeartbeatService(service);
    }

    @Override
    protected void doUnregister(URL url) {
        PortalRegistryService service = PortalRegistryUtils.buildService(url);
        client.unregisterService(service, getPortalToken());
        if(getPortalRegistryConfig().ttlCheck) heartbeatManager.removeHeartbeatService(service);
    }

    @Override
    protected void doAvailable(URL url) {
        if (url == null) {
            if(getPortalRegistryConfig().ttlCheck) heartbeatManager.setHeartbeatOpen(true);
        } else {
            throw new UnsupportedOperationException("Command consul registry not support available by urls yet");
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        if (url == null) {
            if(getPortalRegistryConfig().ttlCheck) heartbeatManager.setHeartbeatOpen(false);
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
     * Override the method in <code>com.networknt.registry.support.commandCommandFailbackRegistry</code>
     * to skip calling the <code>com.networknt.registry.support.commandCommandFailbackRegistry#doDiscover()</code> and
     * <code>com.networknt.registry.support.commandCommandFailbackRegistry#notify()</code>
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
    private void startListenerThreadIfNewService(URL url) {
        String serviceId = url.getPath();
        String tag = url.getParameter(Constants.TAG_ENVIRONMENT);
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        String protocol = url.getProtocol();
        if (!lookupServices.containsKey(key)) {
            Long value = lookupServices.putIfAbsent(key, 0L);
            if (value == null) {
                ServiceLookupThread lookupThread = new ServiceLookupThread(protocol, serviceId, tag);
                lookupThread.setDaemon(true);
                lookupThread.start();
            }
        }
    }

    private void addServiceListener(URL url, ServiceListener serviceListener) {
        String service = PortalRegistryUtils.getUrlClusterInfo(url);
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
        ConcurrentHashMap<URL, ServiceListener> listeners = serviceListeners.get(PortalRegistryUtils.getUrlClusterInfo(url));
        if (listeners != null) {
            synchronized (listeners) {
                listeners.remove(url);
            }
        }
    }

    @Override
    protected List<URL> discoverService(URL url) {
        String serviceId = url.getPath();
        String tag = url.getParameter(Constants.TAG_ENVIRONMENT);
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        String protocol = url.getProtocol();
        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol + " serviceId = " + serviceId + " tag = " + tag);
        List<URL> urls = serviceCache.get(key);
        if (urls == null || urls .isEmpty()) {
            synchronized (key.intern()) {
                urls = serviceCache.get(key);
                if (urls == null || urls .isEmpty()) {
                    ConcurrentHashMap<String, List<URL>> serviceUrls = lookupServiceUpdate(protocol, serviceId, tag);
                    updateServiceCache(serviceId, serviceUrls, false);
                    urls = serviceCache.get(key);
                }
            }
        }
        return urls;
    }

    private ConcurrentHashMap<String, List<URL>> lookupServiceUpdate(String protocol, String serviceId, String tag) {
        if(logger.isTraceEnabled()) logger.trace("protocol = " + protocol  + " serviceId = " + serviceId + " tag = " + tag);
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        PortalRegistryResponse<List<PortalRegistryService>> response = lookupConsulService(serviceId, tag);
        if(logger.isTraceEnabled()) {
            try {
                logger.trace("response = " + Config.getInstance().getMapper().writeValueAsString(response));
            } catch (Exception e) {}
        }
        ConcurrentHashMap<String, List<URL>> serviceUrls = new ConcurrentHashMap<>();
        if (response != null) {
            List<PortalRegistryService> services = response.getValue();
            if(logger.isDebugEnabled()) try {logger.debug("services = " + Config.getInstance().getMapper().writeValueAsString(services));} catch (Exception e) {}
            if (services != null && !services.isEmpty()) {
                for (PortalRegistryService service : services) {
                    try {
                        URL url = PortalRegistryUtils.buildUrl(protocol, service);
                        List<URL> urlList = serviceUrls.get(key);
                        if (urlList == null) {
                            urlList = new ArrayList<>();
                            serviceUrls.put(key, urlList);
                        }
                        if(logger.isTraceEnabled()) logger.trace("lookupServiceUpdate url = " + url);
                        urlList.add(url);
                    } catch (Exception e) {
                        logger.error("convert portal registry service to url fail! service:" + service, e);
                    }
                }
                return serviceUrls;
            } else {
                logger.info(key + " no need update with empty service list");
            }
        } else {
            serviceUrls.put(key, new ArrayList<>());
            logger.info("no response for service: {}, set urls to empty list", key);
        }
        return serviceUrls;
    }

    /**
     * directly fetch consul service data.
     *
     * @param serviceId service Id
     * @return PortalRegistryResponse or null
     */
    private PortalRegistryResponse<List<PortalRegistryService>> lookupConsulService(String serviceId, String tag) {
        PortalRegistryResponse<List<PortalRegistryService>> response = client.lookupHealthService(serviceId, tag, getPortalToken());
        return response;
    }

    /**
     * update service cache of the serviceName.
     * update local cache when service list changed,
     * if need notify, notify service
     *
     * @param serviceName
     * @param serviceUrls
     * @param needNotify
     */
    private void updateServiceCache(String serviceName, ConcurrentHashMap<String, List<URL>> serviceUrls, boolean needNotify) {
        if (serviceUrls != null && !serviceUrls.isEmpty()) {
            List<URL> cachedUrls = serviceCache.get(serviceName);
            List<URL> newUrls = serviceUrls.get(serviceName);
            try {
                logger.trace("serviceUrls = {}", Config.getInstance().getMapper().writeValueAsString(serviceUrls));
            } catch(Exception e) {
            }
            boolean change = true;
            if (PortalRegistryUtils.isSame(newUrls, cachedUrls)) {
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
                logger.info("consul notify urls:" + sb.toString());
            }
        }
    }

    private class ServiceLookupThread extends Thread {
       private String protocol;
       private String serviceId;
       private String tag;
       private String key;

        public ServiceLookupThread(String protocol, String serviceId, String tag) {
            this.protocol = protocol;
            this.serviceId = serviceId;
            this.tag = tag;
            this.key = this.tag == null ? this.serviceId : this.serviceId + "|" + tag;
        }

        @Override
        public void run() {
            logger.info("start service lookup thread. lookup interval: " + lookupInterval + "ms, service: " + serviceId);
            while (true) {
                try {
                    sleep(lookupInterval);
                    ConcurrentHashMap<String, List<URL>> serviceUrls = lookupServiceUpdate(protocol, serviceId, tag);
                    updateServiceCache(key, serviceUrls, true);
                } catch (Throwable e) {
                    logger.error("service lookup thread fail!", e);
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

    private PortalRegistryConfig getPortalRegistryConfig(){
        return (PortalRegistryConfig)Config.getInstance().getJsonObjectConfig(PortalRegistryConfig.CONFIG_NAME, PortalRegistryConfig.class);
    }

    private String getPortalToken() {

        return null;
    }

}
