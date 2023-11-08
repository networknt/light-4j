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
import java.util.Set;

/***
 * A factory that creates raw connections and wraps them in SimpleConnection objects.
 * SimpleConnectionMakers are used by SimpleConnectionStates to create connections.
 *
 */
public interface SimpleConnectionMaker {
    /***
     * Establishes a new connection to a URI.
     * Implementations of SimpleConnectionMaker are used by SimpleConnectionStates as a connection factory.
     *
     * @param createConnectionTimeout the maximum time in seconds to wait for a connection to be established
     * @param isHttp2 if true, SimpleConnectionMaker must attempt to establish an HTTP/2 connection, otherwise it will
     *          attempt to create an HTTP/1.1 connection
     * @param uri the URI to connect to
     * @param allCreatedConnections Implementations of SimpleConnectionMaker are used by SimpleConnectionState to create
     *          connections to arbitrary URIs. In other words, implementations of SimpleConnectionMaker are used by
     *          SimpleConnectionState as a connection factory.
     *
     *          A SimpleConnectionMaker will add all connections it creates to the Set <code>allCreatedConnections</code>.
     *          SimpleURIConnectionPool will compare the connections in this Set to those that it is tracking and close
     *          any untracked connections.
     *
     *          Untracked connections can occur if there is a connection creation timeout. When such a timeout occurs,
     *          makeConnection() must throw a RuntimeException which will prevent SimpleURIConnectionPool from acquiring
     *          a SimpleConnection. However, the connection creation callback thread in makeConnection() may continue to
     *          execute after the timeout and ultimately succeed in creating the connection after the timeout has occurred
     *          and the exception has been thrown. Connections that are created but were not returned to
     *          SimpleURIConnectionPool are considered to be 'untracked'.
     *
     *          Despite not being tracked by SimpleURIConnectionPool, all successfully created connections must be added
     *          to <code>allCreatedConnections</code>.
     *
     *          SimpleURIConnectionPool prevents these untracked connections from accumulating and causing a connection
     *          leak over time, by periodically closing any open connections in allCreatedConnections that it is not tracking.
     *
     *          Thread Safety:
     *              allCreatedConnections MUST be a threadsafe Set, or the thread safety of the connection pool cannot
     *              be guaranteed.
     *
     * @return A SimpleConnection to the specified URI
     * @throws RuntimeException thrown if the connection establishment timeout (<code>createConnectionTimeout</code>)
     *          expires before a connection to the URI is established, or if there is an error establishing the connection
     */
    public SimpleConnection makeConnection(long createConnectionTimeout, boolean isHttp2, final URI uri, final Set<SimpleConnection> allCreatedConnections) throws RuntimeException;
}
