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
     * This is a convenience method that immediately closes a connection even if there are still threads actively
     * using it (i.e: it will be closed even if it is currently borrowed by other threads).
     *
     * 1. WARNING: Connection Token must still be returned
     *     After closing a connection, users must still return the connection token to the pool.
     *
     * 2. WARNING: Overuse of this method will negate all benefits of the connection pool
     *     Under normal circumstances, users should never close connections themselves (either via this method or
     *     directly via the raw connection) but instead, always let the connection pool handle all connection closures.
     *     Manually closing connections negates all benefits of using a connection pool. Be certain that this method is
     *     only used in cases where there is a need to ensure the connection is immediately closed in all threads using it.
     *
     * 3. All threads sharing this connection will be affected
     *     Closing connections yourself (using this method or directly via the raw connection) will cause all threads
     *     that are currently using this connection to experience unexpected connection failures.
     *
     * 4. This method is a convenience method
     *      This method is a convenience method to allow users to close connections via the SimplePool API rather than
     *      closing the raw connection directly. However, if there is a concrete need to do so (see point 2 above) users
     *      can also close raw connections directly at any time without calling this method since SimpleConnectionPool
     *      will safely handle unexpected closures of raw connections. For example:
     *
     *          // get raw Undertow connection
     *          ClientConnection connection = (ClientConnection) connectionToken.getRawConnection();
     *
     *          ...
     *
     *          if(isMajorConnectionIssueDetected)
     *              IoUtils.safeClose(connection);     // safely close the raw connection directly
     *
     *   This means that users can leverage this method to close connections if it is convenient, but can also close
     *   the raw connection directly at any time if that is more convenient (and after considering item 2 above).
     *
     * @param connectionToken the connection token of the connection to close
     */
    public void safeClose(SimpleConnectionState.ConnectionToken connectionToken) {
        if (connectionToken == null || !pools.containsKey(connectionToken.uri()))
            return;

        pools.get(connectionToken.uri()).safeClose(connectionToken);
    }

    /***
     * This method causes the connection to be closed as soon as all threads currently using it have restored it to the
     * pool. This prevents the connection from ever being reused, while also preventing threads that are currently using
     * it from experiencing unexpected connection closures.
     *
     * This method 'expires' a connection which results in:
     *     (a) the connection no longer being borrowable, and
     *     (b) the connection being closed as soon as all threads currently using it have restored it to the pool.
     *
     * 1. WARNING: Connection Token must still be returned
     *     After closing a connection, users must still return the connection token to the pool.
     *
     * 2. WARNING: Overuse of this method will negate all benefits of the connection pool
     *     Under normal circumstances, users should never close connections themselves (either via this method or
     *     directly via the raw connection) but instead, always let the connection pool handle all connection closures.
     *     Manually closing connections negates all benefits of using a connection pool. Be certain that this method is
     *     only used in cases where there is a need to ensure the connection is not reused.
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
