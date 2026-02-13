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

import io.undertow.connector.ByteBufferPool;
import com.networknt.client.simplepool.SimpleConnectionHolder;
import org.xnio.OptionMap;
import org.xnio.XnioWorker;
import org.xnio.ssl.XnioSsl;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Set;

/***
 * A factory that creates raw connections and wraps them in SimpleConnection objects.
 * SimpleConnectionMakers are used by SimpleConnectionHolders to create connections.
 *
 */
public interface SimpleConnectionMaker {
    SimpleConnection makeConnection(long createConnectionTimeout, boolean isHttp2, final URI uri, final Set<SimpleConnection> allCreatedConnections);

    /**
     * This is the method uses the XnioWorker to create the connection.
     * @param createConnectionTimeout in milliseconds
     * @param bindAddress the address to bind to
     * @param uri the uri to connect to
     * @param worker the XnioWorker to use
     * @param ssl the XnioSsl to use
     * @param bufferPool the ByteBufferPool to use
     * @param options the OptionMap to use
     * @param allCreatedConnections the set of all connections created by this SimpleConnectionMaker
     * @return SimpleConnection the connection
     */
    SimpleConnection makeConnection(long createConnectionTimeout, InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options, final Set<SimpleConnection> allCreatedConnections);
    SimpleConnection reuseConnection(SimpleConnection connection) throws RuntimeException;
}
