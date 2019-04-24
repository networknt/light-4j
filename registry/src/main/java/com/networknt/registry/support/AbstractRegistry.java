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

package com.networknt.registry.support;

import com.networknt.utility.Constants;
import com.networknt.registry.URLParamType;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.Registry;
import com.networknt.registry.URL;
import com.networknt.switcher.SwitcherListener;
import com.networknt.utility.ConcurrentHashSet;
import com.networknt.switcher.SwitcherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Abstract registry。
 *
 * Use url createCopy to prevent object modification in multi-thread env
 *
 * @author fishermen
 */

abstract class AbstractRegistry implements Registry {
    private static final Logger logger = LoggerFactory.getLogger(AbstractRegistry.class);

    private ConcurrentHashMap<URL, Map<String, List<URL>>> subscribedCategoryResponses =
            new ConcurrentHashMap<>();

    private URL registryUrl;
    private Set<URL> registeredServiceUrls = new ConcurrentHashSet<>();
    protected String registryClassName = this.getClass().getSimpleName();

    AbstractRegistry(URL url) {
        this.registryUrl = url.createCopy();
        // register a heartbeat switcher to perceive service state change and change available state
        SwitcherUtil.registerSwitcherListener(Constants.REGISTRY_HEARTBEAT_SWITCHER, new SwitcherListener() {

            @Override
            public void onValueChanged(String key, Boolean value) {
                if (key != null && value != null) {
                    if (value) {
                        available(null);
                    } else {
                        unavailable(null);
                    }
                }
            }
        });
    }

    @Override
    public void register(URL url) {
        if (url == null) {
            logger.warn("[{}] register with malformed param, url is null", registryClassName);
            return;
        }
        if(logger.isInfoEnabled()) logger.info("[{}] Url ({}) will register to Registry [{}]",
                registryClassName, url, registryUrl.getIdentity());
        doRegister(removeUnnecessaryParmas(url.createCopy()));
        registeredServiceUrls.add(url);
        // available if heartbeat switcher already open
        if (SwitcherUtil.isOpen(Constants.REGISTRY_HEARTBEAT_SWITCHER)) {
            available(url);
        }
    }

    @Override
    public void unregister(URL url) {
        if (url == null) {
            logger.warn("[{}] unregister with malformed param, url is null", registryClassName);
            return;
        }
        if(logger.isInfoEnabled()) logger.info("[{}] Url ({}) will unregister to Registry [{}]",
                registryClassName, url, registryUrl.getIdentity());
        doUnregister(removeUnnecessaryParmas(url.createCopy()));
        registeredServiceUrls.remove(url);
    }

    @Override
    public void subscribe(URL url, NotifyListener listener) {
        if (url == null || listener == null) {
            logger.warn("[{}] subscribe with malformed param, url:{}, listener:{}",
                    registryClassName, url, listener);
            return;
        }
        if(logger.isInfoEnabled()) logger.info("[{}] Listener ({}) will subscribe to url ({}) in Registry [{}]",
                registryClassName, listener, url, registryUrl.getIdentity());
        doSubscribe(url.createCopy(), listener);
    }

    @Override
    public void unsubscribe(URL url, NotifyListener listener) {
        if (url == null || listener == null) {
            logger.warn("[{}] unsubscribe with malformed param, url:{}, listener:{}", registryClassName, url, listener);
            return;
        }
        if(logger.isInfoEnabled()) logger.info("[{}] Listener ({}) will unsubscribe from url ({}) in Registry [{}]",
                registryClassName, listener, url, registryUrl.getIdentity());
        doUnsubscribe(url.createCopy(), listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<URL> discover(URL url) {
        if (url == null) {
            logger.warn("[{}] discover with malformed param, refUrl is null", registryClassName);
            return Collections.EMPTY_LIST;
        }
        url = url.createCopy();
        List<URL> results = new ArrayList<>();

        Map<String, List<URL>> categoryUrls = subscribedCategoryResponses.get(url);
        if (categoryUrls != null && categoryUrls.size() > 0) {
            for (List<URL> urls : categoryUrls.values()) {
                for (URL tempUrl : urls) {
                    results.add(tempUrl.createCopy());
                }
            }
        } else {
            List<URL> urlsDiscovered = doDiscover(url);
            if (urlsDiscovered != null) {
                for (URL u : urlsDiscovered) {
                    results.add(u.createCopy());
                }
            }
        }
        return results;
    }

    @Override
    public URL getUrl() {
        return registryUrl;
    }


    @Override
    public Collection<URL> getRegisteredServiceUrls() {
        return registeredServiceUrls;
    }

    @Override
    public void available(URL url) {
        if(logger.isInfoEnabled()) logger.info("[{}] Url ({}) will set to available to Registry [{}]",
                registryClassName, url, registryUrl.getIdentity());
        if (url != null) {
            doAvailable(removeUnnecessaryParmas(url.createCopy()));
        } else {
            doAvailable(null);
        }
    }

    @Override
    public void unavailable(URL url) {
        if(logger.isInfoEnabled()) logger.info("[{}] Url ({}) will set to unavailable to Registry [{}]",
                registryClassName, url, registryUrl.getIdentity());
        if (url != null) {
            doUnavailable(removeUnnecessaryParmas(url.createCopy()));
        } else {
            doUnavailable(null);
        }
    }

    List<URL> getCachedUrls(URL url) {
        Map<String, List<URL>> rsUrls = subscribedCategoryResponses.get(url);
        if (rsUrls == null || rsUrls.size() == 0) {
            return null;
        }

        List<URL> urls = new ArrayList<>();
        for (List<URL> us : rsUrls.values()) {
            for (URL tempUrl : us) {
                urls.add(tempUrl.createCopy());
            }
        }
        return urls;
    }

    /*
    protected void notify(URL refUrl, NotifyListener listener, List<URL> urls) {
        if (listener == null || urls == null) {
            return;
        }
        Map<String, List<URL>> nodeTypeUrlsInRs = new HashMap<>();
        for (URL surl : urls) {
            String nodeType = surl.getParameter(URLParamType.nodeType.getName(), URLParamType.nodeType.getValue());
            List<URL> oneNodeTypeUrls = nodeTypeUrlsInRs.get(nodeType);
            if (oneNodeTypeUrls == null) {
                nodeTypeUrlsInRs.put(nodeType, new ArrayList<>());
                oneNodeTypeUrls = nodeTypeUrlsInRs.get(nodeType);
            }
            oneNodeTypeUrls.add(surl);
        }

        Map<String, List<URL>> curls = subscribedCategoryResponses.get(refUrl);
        if (curls == null) {
            subscribedCategoryResponses.putIfAbsent(refUrl, new ConcurrentHashMap<>());
            curls = subscribedCategoryResponses.get(refUrl);
        }

        // refresh local urls cache
        for (String nodeType : nodeTypeUrlsInRs.keySet()) {
            curls.put(nodeType, nodeTypeUrlsInRs.get(nodeType));
        }

        for (List<URL> us : nodeTypeUrlsInRs.values()) {
            listener.notify(getUrl(), us);
        }
    }
    */
    protected void notify(URL refUrl, NotifyListener listener, List<URL> urls) {
        if (listener == null || urls == null) {
            return;
        }
        Map<String, List<URL>> serviceNameUrls = new HashMap<>();
        for (URL surl : urls) {
            String serviceName = surl.getPath();
            List<URL> serviceUrlList = serviceNameUrls.get(serviceName);
            if (serviceUrlList == null) {
                serviceNameUrls.put(serviceName, new ArrayList<>());
                serviceUrlList = serviceNameUrls.get(serviceName);
            }
            serviceUrlList.add(surl);
        }

        Map<String, List<URL>> curls = subscribedCategoryResponses.get(refUrl);
        if (curls == null) {
            subscribedCategoryResponses.putIfAbsent(refUrl, new ConcurrentHashMap<>());
            curls = subscribedCategoryResponses.get(refUrl);
        }

        // refresh local urls cache
        for (String serviceName : serviceNameUrls.keySet()) {
            curls.put(serviceName, serviceNameUrls.get(serviceName));
        }

        for (List<URL> us : serviceNameUrls.values()) {
            listener.notify(getUrl(), us);
        }
    }

    /**
     * client doesn't need to know codec.
     *
     * @param url a URL object
     */
    private URL removeUnnecessaryParmas(URL url) {
        url.getParameters().remove(URLParamType.codec.getName());
        return url;
    }

    protected abstract void doRegister(URL url);

    protected abstract void doUnregister(URL url);

    protected abstract void doSubscribe(URL url, NotifyListener listener);

    protected abstract void doUnsubscribe(URL url, NotifyListener listener);

    protected abstract List<URL> doDiscover(URL url);

    protected abstract void doAvailable(URL url);

    protected abstract void doUnavailable(URL url);

}
