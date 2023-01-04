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

import com.networknt.config.Config;
import com.networknt.registry.URLImpl;
import com.networknt.status.Status;
import com.networknt.exception.FrameworkException;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.URL;
import com.networknt.utility.Constants;
import com.networknt.utility.ModuleRegistry;
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
    private final static String GENERAL_TAG = "*";
    private ConcurrentHashMap<URL, Object> subscribeUrls = new ConcurrentHashMap();
    private static Map<String, List<URL>> directUrls = new HashMap();
    private static DirectRegistryConfig config;

    public DirectRegistry(URL url) {
        super(url);
        config = DirectRegistryConfig.load();
        if(config.directUrls != null) {
            ModuleRegistry.registerModule(DirectRegistry.class.getName(), Config.getInstance().getJsonMapConfigNoCache(DirectRegistryConfig.CONFIG_NAME), null);
        }
        if(url.getParameters() != null && url.getParameters().size() > 0) {
            // The parameters come from the service.yml injection. If it is empty, then load it from the direct-registry.yml
            for (Map.Entry<String, String> entry : url.getParameters().entrySet()) {
                String tag = null;
                try {
                    if (logger.isTraceEnabled())
                        logger.trace("entry key = " + entry.getKey() + " entry value = " + entry.getValue());
                    if (entry.getValue().contains(",")) {
                        String[] directUrlArray = entry.getValue().split(",");
                        for (String directUrl : directUrlArray) {
                            String s = buildUrl(directUrl, entry.getKey());
                            URL u = URLImpl.valueOf(s);
                            tag = u.getParameter(Constants.TAG_ENVIRONMENT);
                            String key = tag == null ? entry.getKey() : entry.getKey() + "|" + tag;
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
                        String s = buildUrl(entry.getValue(), entry.getKey());
                        URL u = URLImpl.valueOf(s);
                        tag = u.getParameter(Constants.TAG_ENVIRONMENT);
                        String key = tag == null ? entry.getKey() : entry.getKey() + "|" + tag;
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

    private String buildUrl(String url, String key) {
        if(url.contains("?")) {
            // allow the environment parameter here as an option to for tag based lookup.
            String u = url.substring(0, url.indexOf("?"));
            String p = url.substring(url.indexOf("?"));
            // insert the path to the middle and move the parameter to the end to form a valid url
            return u.trim() + "/" + key + p;
        } else {
            return url.trim() + "/" + key;
        }
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
        String key = tag == null ? serviceId : serviceId + "|" + tag;
        return directUrls.get(key);
    }

    @Override
    protected void doAvailable(URL url) {
        // do nothing
    }

    @Override
    protected void doUnavailable(URL url) {
        // do nothing
    }

    public static void reload() {
        config.reload();
        directUrls = config.getDirectUrls();
        if(directUrls != null) ModuleRegistry.registerModule(DirectRegistry.class.getName(), Config.getInstance().getJsonMapConfigNoCache(DirectRegistryConfig.CONFIG_NAME), null);
    }
}
