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
import com.networknt.config.JsonMapper;
import com.networknt.portal.registry.client.PortalRegistryClient;
import com.networknt.portal.registry.client.PortalRegistryWebSocketClient;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.URL;
import com.networknt.registry.URLParamType;
import com.networknt.registry.support.AbstractRegistry;
import com.networknt.utility.Constants;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static com.networknt.portal.registry.PortalRegistryConfig.CONFIG_NAME;

public class PortalRegistry extends AbstractRegistry {
    private static final Logger logger = LoggerFactory.getLogger(PortalRegistry.class);
    private static final String CONFIG_PROPERTY_MISSING = "ERR10057";
    private static final PortalRegistryConfig config = (PortalRegistryConfig) Config.getInstance().getJsonObjectConfig(CONFIG_NAME, PortalRegistryConfig.class);
    PortalRegistryWebSocketClient webSocketClient = null;
    private PortalRegistryClient client;
    private PortalRegistryHeartbeatManager heartbeatManager;
    private int lookupInterval;

    // service local cache. key: serviceName, value: <service url list>
    private ConcurrentHashMap<String, List<URL>> serviceCache = new ConcurrentHashMap<String, List<URL>>();

    public PortalRegistry(URL url, PortalRegistryClient client) {
        super(url);
        this.client = client;
        if(getPortalRegistryConfig().ttlCheck) {
            heartbeatManager = new PortalRegistryHeartbeatManager(client, getPortalToken());
            heartbeatManager.start();
        }
        lookupInterval = getUrl().getIntParameter(URLParamType.registrySessionTimeout.getName(), PortalRegistryConstants.DEFAULT_LOOKUP_INTERVAL);
        ArrayBlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(20000);
        logger.info("PortalRegistry init finish.");
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
            throw new UnsupportedOperationException("Portal registry not support available by urls yet");
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        if (url == null) {
            if(getPortalRegistryConfig().ttlCheck) heartbeatManager.setHeartbeatOpen(false);
        } else {
            throw new UnsupportedOperationException("Portal registry not support unavailable by urls yet");
        }
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
        if(logger.isInfoEnabled()) logger.info("PortalRegistry subscribe url: " + url.toSimpleString());
        // here we assume that the first lookup has been done for the url so that subscription info is available on the server
        // We just need to open the WebSocket connection here if it is not created. Both parameters are not used here and we
        // can save a lot of threads than the Consul implementation.
        try {
            String u = "wss" + config.getPortalUrl().substring(config.getPortalUrl().indexOf("://"));
            if(webSocketClient == null) {
                // The client is created only once and it is responsible for all the subscriptions to the down stream APIs.
                webSocketClient = new PortalRegistryWebSocketClient(URI.create(u + "/ws")) {
                    @Override
                    public void onOpen() {
                        System.out.println("open");
                    }

                    @Override
                    public void onMessage(String msg) {
                        if(logger.isDebugEnabled()) logger.debug("WebSocket message: " + msg);
                        // The subscribed service nodes have been changed.
                        // {"com.networknt.ab-1.0.0|test1":[]}
                        updateCacheFromMessage(msg);
                    }

                    @Override
                    public void onClose(int code, String reason) {
                        System.out.println("close (code: " + code + ", reason: " + reason + ")");
                    }

                    @Override
                    public void onError(Exception e) {
                        System.out.println("err: " + e.getMessage());
                        System.out.println("Client isOpen?: " + this.isOpen());
                    }
                };
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        webSocketClient.send(url.toFullStr());
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        // same as the doSubscribe, both url and listener are not used but it is called per client url and it can be
        // called multiple time without any issue. The first call will stop the WebSocket client.
        if(logger.isInfoEnabled()) logger.info("PortalRegistry unsubscribe url: " + url.toSimpleString());
        try {
            if(webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.close();
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
    }

    @Override
    protected List<URL> doDiscover(URL url) {
        String serviceId = url.getPath();
        String tag = url.getParameter(Constants.TAG_ENVIRONMENT);
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        String protocol = url.getProtocol();
        if(logger.isTraceEnabled()) logger.trace("discover protocol = " + protocol + " serviceId = " + serviceId + " tag = " + tag);
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
        List<Map<String, Object>> services = lookupService(serviceId, tag);
        return convertLisMap2UR(serviceId, tag, protocol, services);
    }

    private void updateCacheFromMessage(String message) {
        Map<String, Object> map = JsonMapper.string2Map(message);
        // there is only one entry in the map from the socket message.
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        Map.Entry<String, Object> entry = iterator.next();
        String key = entry.getKey();

        String serviceId = null;
        String tag = null;
        if(key.indexOf("|") > 0) {
            String[] parts = StringUtils.split(key, "|");
            serviceId = parts[0];
            tag = parts[1];
        } else {
            serviceId = key;
        }

        List nodes = (List)entry.getValue();
        ConcurrentHashMap<String, List<URL>> serviceUrls = convertLisMap2UR(serviceId,  tag, null, nodes);
        synchronized (key.intern()) {
            updateServiceCache(key, serviceUrls, false);
        }
    }

    private ConcurrentHashMap<String, List<URL>> convertLisMap2UR(String serviceId, String tag, String protocol, List<Map<String, Object>> services)  {
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        ConcurrentHashMap<String, List<URL>> serviceUrls = new ConcurrentHashMap<>();
        if (services != null && !services.isEmpty()) {
            for (Map<String, Object> service : services) {
                try {
                    URL url = PortalRegistryUtils.buildUrl(serviceId, tag, service);
                    // filter the protocol base on the passed in parameter
                    if(protocol != null && !url.getProtocol().equals(protocol)) continue;
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
            serviceUrls.put(key, new ArrayList<>());
            logger.info("no response for service: {}, set urls to empty list", key);
        }
        return serviceUrls;
    }

    /**
     * directly fetch portal registry service data.
     *
     * @param serviceId service Id
     * @return list of services or null
     */
    private List<Map<String, Object>> lookupService(String serviceId, String tag) {
        return client.lookupHealthService(serviceId, tag, getPortalToken());
    }

    /**
     * update service cache of the service key.
     * update local cache when service list changed,
     * if need notify, notify service
     *
     * @param key service key with serviceId and optional tag
     * @param serviceUrls
     * @param needNotify
     */
    private void updateServiceCache(String key, ConcurrentHashMap<String, List<URL>> serviceUrls, boolean needNotify) {
        if (serviceUrls != null && !serviceUrls.isEmpty()) {
            List<URL> cachedUrls = serviceCache.get(key);
            List<URL> newUrls = serviceUrls.get(key);
            if(logger.isTraceEnabled()) logger.trace("serviceUrls = " + JsonMapper.toJson(serviceUrls));
            boolean change = true;
            if (PortalRegistryUtils.isSame(newUrls, cachedUrls)) {
                change = false;
            } else {
                serviceCache.put(key, newUrls);
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
