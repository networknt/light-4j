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

import com.networknt.registry.URLImpl;
import com.networknt.status.Status;
import com.networknt.exception.FrameworkException;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.URL;
import com.networknt.utility.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Direct registry is used for local testing only. It is implement all the
 * interface of registry and discovery that is backed by local service.yml
 * configuration. All instances of the service will be defined in the config
 * as hard-coded url:port along with other parameters.
 *
 * @author axb, Steve Hu
 */
public class DirectRegistry extends AbstractRegistry {
    private final static Logger logger = LoggerFactory.getLogger(DirectRegistry.class);
    private final static String PARSE_DIRECT_URL_ERROR = "ERR10019";
    private final ConcurrentHashMap<URL, Object> subscribeUrls = new ConcurrentHashMap<>();
    private Map<String, List<URL>> directUrls;
    private volatile DirectRegistryConfig config;

    /**
     * Constructs a DirectRegistry with a URL.
     * Loads the direct urls from the direct-registry configuration.
     *
     * @param url registry URL
     */
    public DirectRegistry(URL url) {
        super(url);
        config = DirectRegistryConfig.load();
        if(url.getParameters() != null && !url.getParameters().isEmpty()) {
            logger.warn("Parameter is used for DirectRegistry and it cannot be reloaded. Please switch to direct-registry.yml file.");
            directUrls = new HashMap<>();
            // The parameters come from the service.yml injection. If it is empty, then load it from the direct-registry.yml
            for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                String tag = null;
                try {
                    if (logger.isTraceEnabled())
                        logger.trace("entry key = {} entry value = {}", entry.getKey(), entry.getValue());
                    if (entry.getValue().contains(",")) {
                        String[] directUrlArray = entry.getValue().split(",");
                        for (String directUrl : directUrlArray) {
                            URL u = buildUrl(directUrl, entry.getKey());
                            tag = u.getParameter(Constants.TAG_ENVIRONMENT);
                            String key = serviceKey(entry.getKey(), tag);
                            List<URL> urls = directUrls.get(key);
                            if (urls != null) {
                                urls.add(u);
                            } else {
                                urls = new ArrayList<>();
                                urls.add(u);
                            }
                            directUrls.put(key, urls);
                        }
                    } else {
                        List<URL> urls = new ArrayList<>();
                        URL u = buildUrl(entry.getValue(), entry.getKey());
                        tag = u.getParameter(Constants.TAG_ENVIRONMENT);
                        String key = serviceKey(entry.getKey(), tag);
                        urls.add(u);
                        directUrls.put(key, urls);
                    }
                } catch (Exception e) {
                    logger.error("Exception: ", e);
                    throw new FrameworkException(new Status(PARSE_DIRECT_URL_ERROR, url.toString()));
                }
            }
        } else {
            // load from the direct-registry.yml file for the directUrls.
            directUrls = config.getDirectUrls();
        }
    }

    /**
     * Build the URL of a service instance from a direct url defined in the service.yml parameters. The serviceId is
     * the path of the URL for the registry, so the path of the direct url, which is the base path of a service behind
     * a path based k8s ingress, is set as the basePath parameter of the URL object. It cannot be serialized into the
     * query string of the url as a base path may contain a query delimiter that would be parsed as another parameter.
     *
     * @param url the direct url of the service instance
     * @param key the serviceId, optionally with the environment tag, of the entry in the parameters
     * @return the URL of the service instance with an optional basePath parameter
     */
    private URL buildUrl(String url, String key) {
        String u = url.trim();
        String p = "";
        int q = u.indexOf("?");
        if(q >= 0) {
            // allow the environment parameter here as an option to for tag based lookup.
            p = u.substring(q);
            u = u.substring(0, q).trim();
        }
        String basePath = null;
        int i = u.indexOf(Constants.PROTOCOL_SEPARATOR);
        int s = u.indexOf(Constants.PATH_SEPARATOR, i >= 0 ? i + Constants.PROTOCOL_SEPARATOR.length() : 0);
        if(s >= 0) {
            basePath = DirectRegistryConfig.normalizeBasePath(u.substring(s));
            u = u.substring(0, s);
        }
        // insert the path to the middle and move the parameter to the end to form a valid url
        URL result = URLImpl.valueOf(u + Constants.PATH_SEPARATOR + key + p);
        if(basePath != null) {
            result.addParameter(Constants.BASE_PATH, basePath);
        }
        return result;
    }

    @Override
    protected void doRegister(URL url) {
        // do nothing
    }

    @Override
    protected void doUnregister(URL url) {
        // do nothing
    }

    @Override
    protected void doSubscribe(URL url, NotifyListener listener) {
        subscribeUrls.putIfAbsent(url, 1);
        if(listener != null) listener.notify(this.getUrl(), doDiscover(url));
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        subscribeUrls.remove(url);
        if(listener != null) listener.notify(this.getUrl(), doDiscover(url));
    }

    @Override
    protected List<URL> doDiscover(URL subscribeUrl) {
        return createSubscribeUrl(subscribeUrl);
    }

    private List<URL> createSubscribeUrl(URL subscribeUrl) {
        String serviceId = subscribeUrl.getPath();
        String tag = subscribeUrl.getParameter(Constants.TAG_ENVIRONMENT);
        // reload DirectRegistryConfig to check if the cached object is changed.
        DirectRegistryConfig newConfig = DirectRegistryConfig.load();
        if (newConfig != config) {
            synchronized (DirectRegistry.class) {
                if (newConfig != config) {
                    config = newConfig;
                    directUrls = config.getDirectUrls();
                    if(directUrls.isEmpty()) {
                        logger.error("direct-registry.directUrls is empty in values.yml.");
                    }
                }
            }
        }
        return directUrls.get(serviceKey(serviceId, tag));
    }

    @Override
    protected void doAvailable(URL url) {
        // do nothing
    }

    @Override
    protected void doUnavailable(URL url) {
        // do nothing
    }

}
