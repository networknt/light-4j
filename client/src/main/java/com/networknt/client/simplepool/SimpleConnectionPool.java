/*
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
 *
 * @author miklish Michael N. Christoff
 *
 * testing / QA
 *   AkashWorkGit
 *   jaydeepparekh1311
 */
package com.networknt.client.simplepool;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a class through which multiple URI Connection Pools can be accessed
 */
public final class SimpleConnectionPool {
    private final Map<URI, SimpleURIConnectionPool> pools = new ConcurrentHashMap<>();
    private final SimpleConnectionMaker connectionMaker;
    private final long expireTime;
    private final int poolSize;

    public SimpleConnectionPool(long expireTime, int poolSize, SimpleConnectionMaker connectionMaker) {
        this.expireTime = expireTime;
        this.poolSize = poolSize;
        this.connectionMaker = connectionMaker;
    }

    public SimpleConnectionHolder.ConnectionToken borrow(long createConnectionTimeout, boolean isHttp2, URI uri)
        throws RuntimeException
    {
        if(!pools.containsKey(uri)) {
            synchronized (pools) {
                if (!pools.containsKey(uri))
                    pools.put(uri, new SimpleURIConnectionPool(uri, expireTime, poolSize, connectionMaker));
            }
        }
        return pools.get(uri).borrow(createConnectionTimeout);
    }

    public void restore(SimpleConnectionHolder.ConnectionToken connectionToken) {
        if(pools.containsKey(connectionToken.uri()))
            pools.get(connectionToken.uri()).restore(connectionToken);
    }
}
