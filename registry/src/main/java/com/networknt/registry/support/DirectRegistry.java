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

import com.networknt.status.Status;
import com.networknt.utility.Constants;
import com.networknt.exception.FrameworkException;
import com.networknt.registry.NotifyListener;
import com.networknt.registry.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by axb on 16/6/12.
 */
public class DirectRegistry extends AbstractRegistry {
    private final static String PARSE_DIRECT_URL_ERROR = "ERR10019";
    private ConcurrentHashMap<URL, Object> subscribeUrls = new ConcurrentHashMap();
    private List<URL> directUrls = new ArrayList<>();

    public DirectRegistry(URL url) {
        super(url);
        String address = url.getParameter("address");
        if (address.contains(",")) {
            try {
                String[] directUrlArray = address.split(",");
                for (String directUrl : directUrlArray) {
                    parseDirectUrl(directUrl);
                }
            } catch (Exception e) {
                throw new FrameworkException(new Status(PARSE_DIRECT_URL_ERROR));
            }
        } else {
            registerDirectUrl(url.getHost(), url.getPort());
        }
    }

    private void parseDirectUrl(String directUrl) {
        String[] ipAndPort = directUrl.split(":");
        String ip = ipAndPort[0];
        Integer port = Integer.parseInt(ipAndPort[1]);
        if (port < 0 || port > 65535) {
            throw new RuntimeException();
        }
        registerDirectUrl(ip, port);
    }

    private void registerDirectUrl(String ip, Integer port) {
        URL url = new URL(Constants.REGISTRY_PROTOCOL_DIRECT,ip,port,"");
        directUrls.add(url);
    }

    private void parseIpAndPort(String directUrl) {
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
        URL url = this.getUrl();
        List result = new ArrayList(directUrls.size());
        for (URL directUrl : directUrls) {
            URL tmp = subscribeUrl.createCopy();
            tmp.setHost(directUrl.getHost());
            tmp.setPort(directUrl.getPort());
            result.add(tmp);
        }
        return result;
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
