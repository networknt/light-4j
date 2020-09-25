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
    private final static String PARSE_DIRECT_URL_ERROR = "ERR10019";
    private final static String GENERAL_TAG = "*";
    private ConcurrentHashMap<URL, Object> subscribeUrls = new ConcurrentHashMap();
    private Map<String, List<URL>> directUrls = new HashMap();

    public DirectRegistry(URL url) {
        super(url);
        for (Map.Entry<String, String> entry : url.getParameters().entrySet())
        {
            List<URL> urls = new ArrayList<>();
            try {
                if(entry.getValue().contains(",")) {
                    String[] directUrlArray = entry.getValue().split(",");
                    for (String directUrl : directUrlArray) {
                        String s = "";
                        if(directUrl.contains("?")) {
                            // allow the environment parameter here as an option to for tag based lookup.
                            String u = directUrl.substring(0, directUrl.indexOf("?"));
                            String p = directUrl.substring(directUrl.indexOf("?"));
                            // insert the path to the middle and move the parameter to the end to form a valid url
                            s = u.trim() + "/" + entry.getKey() + p;
                        } else {
                            s = directUrl.trim() + "/" + entry.getKey();
                        }
                        urls.add(addGeneralTag(URLImpl.valueOf(s)));
                    }
                } else {
                    urls.add(addGeneralTag(URLImpl.valueOf(entry.getValue() + "/" + entry.getKey())));
                }
            } catch (Exception e) {
                throw new FrameworkException(new Status(PARSE_DIRECT_URL_ERROR, url.toString()));
            }
            directUrls.put(entry.getKey(), urls);
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
        listener.notify(this.getUrl(), doDiscover(url));
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        subscribeUrls.remove(url);
        listener.notify(this.getUrl(), doDiscover(url));
    }

    @Override
    protected List<URL> doDiscover(URL subscribeUrl) {
        return createSubscribeUrl(subscribeUrl);
    }

    private List<URL> createSubscribeUrl(URL subscribeUrl) {
        String serviceName = subscribeUrl.getPath();
        // still return everything and the LightCluster will filter out others with the environment tag
        return directUrls.get(serviceName);
    }

    @Override
    protected void doAvailable(URL url) {
        // do nothing
    }

    @Override
    protected void doUnavailable(URL url) {
        // do nothing
    }

    private URL addGeneralTag(URL url) {
        // allow the environment parameter here. use general tag only if it is missing.
        if(url.getParameter(Constants.TAG_ENVIRONMENT) == null) {
            url.addParameter(Constants.TAG_ENVIRONMENT, GENERAL_TAG);
        }
        return url;
    }
}
