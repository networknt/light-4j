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

import java.util.List;

/**
 * To obtain maximum scalability, microservices allow Y-Axis scale to break up big
 * monolithic application to small functional units. However, for some of the heavy
 * load services, we can use data sharding for Z-Axis scale. This load balance is
 * designed for that.
 *
 * In normal case, the requestKey should be client_id or user_id from JWT token, this
 * can guarantee that the same client will always to be routed to one service instance
 * or one user always to be routed to one service instance. However, this key can be a
 * combination of multiple fields from the request.
 *
 *
 * Created by steve on 07/05/17.
 */
public class ConsistentHashLoadBalance implements LoadBalance {
    static Logger logger = LoggerFactory.getLogger(ConsistentHashLoadBalance.class);

    // TODO need to keep a lookup table to map the hash to host:ip, using index in the
    // urls is not reliable as the sequence will be changed after service restart.
    // maybe we need somehow to have an instanceId for each service instance. UUID will
    // do the job. It will be registered as extra parameter like public key and public
    // key certificate of the service.

    public ConsistentHashLoadBalance() {
        if(logger.isInfoEnabled()) logger.info("A ConsistentHashLoadBalance instance is started");
    }


    @Override
    public URL select(List<URL> urls, String requestKey) {
        URL url = null;
        if (urls.size() > 1) {
            url = doSelect(urls, requestKey);
        } else if (urls.size() == 1) {
            url = urls.get(0);
        }
        return url;
    }

    private URL doSelect(List<URL> urls, String requestKey) {
        int hash = getHash(requestKey);
        // convert hash to an index in urls. This assumes there are the same number
        // This will be changed later on.
        return urls.get(hash % urls.size());
    }

    private int getHash(String hashKey) {
        int hashcode;
        if(hashKey != null) {
            hashcode = hashKey.hashCode();
        } else {
            hashcode = 0;
        }
        return getPositive(hashcode);
    }
}
