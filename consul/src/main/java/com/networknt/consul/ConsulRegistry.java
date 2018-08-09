package com.networknt.consul;

import com.networknt.client.Http2Client;
import com.networknt.common.DecryptUtil;
import com.networknt.common.SecretConstants;
import com.networknt.config.Config;
import com.networknt.registry.URLParamType;
import com.networknt.consul.client.ConsulClient;
import com.networknt.registry.support.command.CommandFailbackRegistry;
import com.networknt.registry.support.command.ServiceListener;
import com.networknt.registry.URL;
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

public class ConsulRegistry extends CommandFailbackRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ConsulRegistry.class);
    private ConsulClient client;
    private ConsulHeartbeatManager heartbeatManager;
    private int lookupInterval;
    private Map<String, Object> secret = DecryptUtil.decryptMap(Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_SECRET));
    private String token = secret == null? null : (String)secret.get(SecretConstants.CONSUL_TOKEN);
    private ConsulConfig config = (ConsulConfig)Config.getInstance().getJsonObjectConfig(ConsulConstants.CONFIG_NAME, ConsulConfig.class);

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
        if(config.ttlCheck) {
            heartbeatManager = new ConsulHeartbeatManager(client, token);
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
        client.registerService(service, token);
        if(config.ttlCheck) heartbeatManager.addHeartbeatServcieId(service.getId());
    }

    @Override
    protected void doUnregister(URL url) {
        ConsulService service = ConsulUtils.buildService(url);
        client.unregisterService(service.getId(), token);
        if(config.ttlCheck) heartbeatManager.removeHeartbeatServiceId(service.getId());
    }

    @Override
    protected void doAvailable(URL url) {
        if (url == null) {
            if(config.ttlCheck) heartbeatManager.setHeartbeatOpen(true);
        } else {
            throw new UnsupportedOperationException("Command consul registry not support available by urls yet");
        }
    }

    @Override
    protected void doUnavailable(URL url) {
        if (url == null) {
            if(config.ttlCheck) heartbeatManager.setHeartbeatOpen(false);
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
     * if new service registered, start a new lookup thread
     * each serviceName start a lookup thread to discover service
     *
     * @param url
     */
    private void startListenerThreadIfNewService(URL url) {
        String serviceName = url.getPath();
        String tag = url.getParameter(Constants.TAG_ENVIRONMENT);
        if (!lookupServices.containsKey(serviceName)) {
            Long value = lookupServices.putIfAbsent(serviceName, 0L);
            if (value == null) {
                ServiceLookupThread lookupThread = new ServiceLookupThread(serviceName, tag);
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
        if(logger.isDebugEnabled()) logger.debug("serviceName = " + serviceName + " tag = " + tag);
        List<URL> urls = serviceCache.get(serviceName);
        if (urls == null) {
            synchronized (serviceName.intern()) {
                urls = serviceCache.get(serviceName);
                if (urls == null) {
                    ConcurrentHashMap<String, List<URL>> serviceUrls = lookupServiceUpdate(serviceName, tag);
                    updateServiceCache(serviceName, serviceUrls, false);
                    urls = serviceCache.get(serviceName);
                }
            }
        }
        return urls;
    }

    private ConcurrentHashMap<String, List<URL>> lookupServiceUpdate(String serviceName, String tag) {
        Long lastConsulIndexId = lookupServices.get(serviceName) == null ? 0L : lookupServices.get(serviceName);
        if(logger.isDebugEnabled()) logger.debug("serviceName = " + serviceName + " tag = " + tag + " lastConsulIndexId = " + lastConsulIndexId);
        ConsulResponse<List<ConsulService>> response = lookupConsulService(serviceName, tag, lastConsulIndexId);
        if(logger.isDebugEnabled()) {
            try {
                logger.debug("response = " + Config.getInstance().getMapper().writeValueAsString(response));
            } catch (Exception e) {}
        }
        if (response != null) {
            List<ConsulService> services = response.getValue();
            if(logger.isDebugEnabled()) try {logger.debug("services = " + Config.getInstance().getMapper().writeValueAsString(services));} catch (Exception e) {}
            if (services != null && !services.isEmpty()
                    && response.getConsulIndex() > lastConsulIndexId) {
                ConcurrentHashMap<String, List<URL>> serviceUrls = new ConcurrentHashMap<String, List<URL>>();
                for (ConsulService service : services) {
                    try {
                        URL url = ConsulUtils.buildUrl(service);
                        List<URL> urlList = serviceUrls.get(serviceName);
                        if (urlList == null) {
                            urlList = new ArrayList<>();
                            serviceUrls.put(serviceName, urlList);
                        }
                        if(logger.isDebugEnabled()) logger.debug("lookupServiceUpdate url = " + url);
                        urlList.add(url);
                    } catch (Exception e) {
                        logger.error("convert consul service to url fail! service:" + service, e);
                    }
                }
                lookupServices.put(serviceName, response.getConsulIndex());
                return serviceUrls;
            } else {
                logger.info(serviceName + " no need update, lastIndex:" + lastConsulIndexId);
            }
        }
        return null;
    }

    /**
     * directly fetch consul service data.
     *
     * @param serviceName
     * @return ConsulResponse or null
     */
    private ConsulResponse<List<ConsulService>> lookupConsulService(String serviceName, String tag,  Long lastConsulIndexId) {
        ConsulResponse<List<ConsulService>> response = client.lookupHealthService(serviceName, tag, lastConsulIndexId, token);
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
            List<URL> urls = serviceCache.get(serviceName);
            if (urls == null) {
                if(logger.isDebugEnabled()) {
                    try {
                        logger.debug("serviceUrls = " + Config.getInstance().getMapper().writeValueAsString(serviceUrls));
                    } catch(Exception e) {
                    }
                }
                serviceCache.put(serviceName, serviceUrls.get(serviceName));
            }
            for (Map.Entry<String, List<URL>> entry : serviceUrls.entrySet()) {
                boolean change = true;
                if (urls != null) {
                    List<URL> newUrls = entry.getValue();
                    if (newUrls == null || newUrls.isEmpty() || ConsulUtils.isSame(newUrls, urls)) {
                        change = false;
                    } else {
                        serviceCache.put(serviceName, newUrls);
                    }
                }
                if (change && needNotify) {
                    notifyExecutor.execute(new NotifyService(entry.getKey(), entry.getValue()));
                    logger.info("light service notify-service: " + entry.getKey());
                    StringBuilder sb = new StringBuilder();
                    for (URL url : entry.getValue()) {
                        sb.append(url.getUri()).append(";");
                    }
                    logger.info("consul notify urls:" + sb.toString());
                }
            }
        }
    }

    private class ServiceLookupThread extends Thread {
        private String serviceName;
        private String tag;

        public ServiceLookupThread(String serviceName, String tag) {
            this.serviceName = serviceName;
            this.tag = tag;
        }

        @Override
        public void run() {
            logger.info("start service lookup thread. lookup interval: " + lookupInterval + "ms, service: " + serviceName + ", tag: " + tag);
            while (true) {
                try {
                    sleep(lookupInterval);
                    ConcurrentHashMap<String, List<URL>> serviceUrls = lookupServiceUpdate(serviceName, tag);
                    updateServiceCache(serviceName, serviceUrls, true);
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
}
