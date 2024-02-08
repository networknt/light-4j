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
 *     SimpleConnectionPool is a connection pool, which means that it manages a pool of reusable network connections.
 *     Threads borrow connections from the pool, use them, and then return them back to the pool. Once returned, these
 *     connections can then be borrowed by other threads. Since these returned connections are already active / established,
 *     they can be used immediately without going through the lengthy connection establishment process. This can very
 *     significantly increase the performance of systems that make many simultaneous outgoing API calls.
 */
public final class SimpleConnectionPool {
    private final Map<URI, SimpleURIConnectionPool> pools = new ConcurrentHashMap<>();
    private final SimpleConnectionMaker connectionMaker;
    private final long expireTime;
    private final int poolSize;

    /***
     * Creates a SimpleConnectionPool
     *
     * @param expireTime the length of time in milliseconds a connection is eligible to be borrowed
     * @param poolSize the maximum number of unexpired connections the pool can hold at a given time
     * @param connectionMaker a class that SimpleConnectionPool uses to create new connections
     */
    public SimpleConnectionPool(long expireTime, int poolSize, SimpleConnectionMaker connectionMaker) {
        this.expireTime = expireTime;
        this.poolSize = poolSize;
        this.connectionMaker = connectionMaker;
    }

    /***
     * Returns a network connection to a URI
     *
     * @param createConnectionTimeout The maximum time in seconds to wait for a new connection to be established before
     *          throwing an exception
     * @param isHttp2 if true, SimpleURIConnectionPool will attempt to establish an HTTP/2 connection, otherwise it will
     *          attempt to create an HTTP/1.1 connection
     * @return  a ConnectionToken object that contains the borrowed connection. The thread using the connection must
     *          return this connection to the pool when it is done with it by calling the borrow() method with the
     *          ConnectionToken as the argument
     * @throws RuntimeException if connection creation takes longer than <code>createConnectionTimeout</code> seconds,
     *          or other issues that prevent connection creation
     */
    public SimpleConnectionState.ConnectionToken borrow(long createConnectionTimeout, boolean isHttp2, URI uri)
            throws RuntimeException
    {
        if(!pools.containsKey(uri))
            pools.computeIfAbsent(uri, pool -> new SimpleURIConnectionPool(uri, expireTime, poolSize, connectionMaker));
        
        return pools.get(uri).borrow(createConnectionTimeout, isHttp2);
    }

    /***
     * Restores borrowed connections
     *
     * NOTE: A connection that unexpectedly closes may be removed from connection pool tracking before all of its
     *       ConnectionTokens have been restored. This can result in seeing log messages about CLOSED connections
     *       being restored to the pool that are no longer tracked / known by the connection pool
     *
     * @param connectionToken the connection token that represents the borrowing of a connection by a thread
     */
    public void restore(SimpleConnectionState.ConnectionToken connectionToken) {
        if(connectionToken == null)
            return;

        if(pools.containsKey(connectionToken.uri()))
            pools.get(connectionToken.uri()).restore(connectionToken);
    }

    /***
     * This method immediately closes the connection even if there are still threads actively using it (i.e: it
     * will be closed even if it is still borrowed).
     *
     * WARNING: Closing connections defeats the entire purpose of using a connection pool. Be certain that this method
     *          is only used in cases where there is a need to ensure the connection is not reused. Needing to close
     *          connections after every use prevents a connection pool from being able to provide any of the connection
     *          time performance benefits that are the entire purpose of connection pools.
     *
     * NOTE:    It is NOT necessary to use this method to close connections. SimpleConnectionPool and
     *          SimpleURIConnectionPool are specifically designed to gracefully handle unexpected connections closures.
     *
     *          So feel free to directly close raw connections if that simplifies your code.
     *
     * WARNING: YOU MUST STILL RESTORE THE CONNECTION TOKEN AFTER CALLING THIS METHOD
     *
     * WARNING: This will cause any threads that are actively using this connection to experience unexpected connection
     *          failures
     *
     * @param connectionToken the connection token of the connection to close
     */
    public void safeClose(SimpleConnectionState.ConnectionToken connectionToken) {
        if (connectionToken == null || !pools.containsKey(connectionToken.uri()))
            return;

        pools.get(connectionToken.uri()).safeClose(connectionToken);
    }

    /***
     * Causes the connection to be closed and its resources being freed from the pool, while preventing threads
     * that are currently using it from experiencing unexpected connection closures.
     *
     * WARNING: Closing connections defeats the entire purpose of using a connection pool. Be certain that this method
     *          is only used in cases where there is a need to ensure the connection is not reused. Needing to close
     *          connections after every use prevents a connection pool from being able to provide any of the connection
     *          time performance benefits that are the entire purpose of connection pools.
     *
     * This method expires a connection which results in:
     *     (a) the connection no longer being borrowable, and
     *     (b) the connection being closed as soon as all threads currently using it have restored it to the pool.
     *
     * WARNING: YOU MUST STILL RESTORE THE CONNECTION TOKEN AFTER CALLING THIS METHOD
     *
     * @param connectionToken the connection token for the connection to close
     * @return true if the connection has been closed;
     *         false if (1) the connection is still open due to there being threads that are still actively using it,
     *         or (2) if the connectionToken was null
     */
    public boolean scheduleSafeClose(SimpleConnectionState.ConnectionToken connectionToken) {
        if(connectionToken == null || !pools.containsKey(connectionToken.uri()))
            return false;

        return pools.get(connectionToken.uri()).scheduleSafeClose(connectionToken);
    }
}
