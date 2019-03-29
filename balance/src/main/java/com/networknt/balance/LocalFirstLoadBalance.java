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

package com.networknt.balance;

import com.networknt.registry.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.utility.Util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Local first load balance give local service high priority than remote services.
 * If there is no local service available, then it will adapt round robin strategy.
 *
 * With all the services in the list of urls, find local services with IP. Chances are
 * we have multiple local services, then round robin will be used in this case. If
 * there is no local service, find the first remote service according to round robin.
 *
 * Created by dan on 2016-12-29
 */
public class LocalFirstLoadBalance extends RoundRobinLoadBalance {
    static Logger logger = LoggerFactory.getLogger(LocalFirstLoadBalance.class);

    static String ip = "0.0.0.0";

    static{
    	// get the address of the localhost
        InetAddress inetAddress = Util.getInetAddress();
        // get ip address for this host.
        ip = inetAddress.getHostAddress();
    }

    public LocalFirstLoadBalance() {
        if(logger.isInfoEnabled()) logger.info("A LocalFirstLoadBalance instance is started");
    }

    /**
     * Local first requestKey is not used as it is ip on the localhost. It first needs to
     * find a list of urls on the localhost for the service, and then round robin in the
     * list to pick up one.
     *
     * Currently, this load balance is only used if you deploy the service as standalone
     * java process on data center hosts. We need to find a way to identify two VMs or two
     * docker containers sitting on the same physical machine in the future to improve it.
     *
     * It is also suitable if your services are built on top of light-hybrid-4j and want
     * to use the remote interface for service to service communication.
     *
     * @param urls List
     * @param requestKey String
     * @return URL
     */
    @Override
    public URL select(List<URL> urls, String requestKey) {
    	// search for a URL in the same ip first
        List<URL> localUrls = searchLocalUrls(urls, ip);
        if(localUrls.size() > 0) {
             if(localUrls.size() == 1) {
                 return localUrls.get(0);
             } else {
                // round robin within localUrls
                 return doSelect(localUrls);
             }
        } else {
            // round robin within urls
            return doSelect(urls);
        }
    }

    private List<URL> searchLocalUrls(List<URL> urls, String ip) {
        List<URL> localUrls = new ArrayList<URL>();
        long local = ipToLong(ip);
        for (URL url : urls) {
            long tmp = ipToLong(url.getHost());
            if (local != 0 && local == tmp) {
                localUrls.add(url);
            }
        }
        return localUrls;
    }

    public static long ipToLong(final String address) {
        final String[] addressBytes = address.split("\\.");
        int length = addressBytes.length;
        if (length < 3) {
            return 0;
        }
        long ip = 0;
        try {
            for (int i = 0; i < 4; i++) {
                ip <<= 8;
                ip |= Integer.parseInt(addressBytes[i]);
            }
        } catch (Exception e) {
            logger.warn("Warn ipToLong address is wrong: address =" + address);
        }
        return ip;
    }
}
