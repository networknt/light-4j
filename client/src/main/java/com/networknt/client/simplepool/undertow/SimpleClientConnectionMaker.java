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
package com.networknt.client.simplepool.undertow;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.simplepool.SimpleConnection;
import com.networknt.client.simplepool.SimpleConnectionMaker;
import com.networknt.client.simplepool.exceptions.*;
import io.undertow.Undertow;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.UndertowClient;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.DefaultByteBufferPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;
import org.xnio.ssl.XnioSsl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SimpleClientConnectionMaker implements SimpleConnectionMaker
{
    private static final Logger logger = LoggerFactory.getLogger(SimpleClientConnectionMaker.class);
    private static final ByteBufferPool BUFFER_POOL = new DefaultByteBufferPool(true, ClientConfig.get().getBufferSize() * 1024);
    private static final int DEFAULT_WORKER_IO_THREADS = 8;

    // Thread-safe singleton using Holder pattern
    private static class Holder {
        static final SimpleClientConnectionMaker INSTANCE = new SimpleClientConnectionMaker();
    }

    public static SimpleConnectionMaker instance() {
        return Holder.INSTANCE;
    }

    @Override
    public SimpleConnection makeConnection(
        long createConnectionTimeout,
        boolean isHttp2,
        final URI uri,
        final Set<SimpleConnection> allCreatedConnections) throws RuntimeException
    {
        boolean isHttps = uri.getScheme().equalsIgnoreCase("https");
        XnioWorker worker = getWorker();
        XnioSsl ssl = getSSL(isHttps);
        OptionMap connectionOptions = getConnectionOptions(isHttp2);
        InetSocketAddress bindAddress = null;

        final FutureResult<SimpleConnection> result = new FutureResult<>();
        ClientCallback<ClientConnection> connectionCallback = new ClientCallback<ClientConnection>() {
            @Override
            public void completed(ClientConnection connection) {
                logger.debug("New connection {} established with {}", port(connection), uri);
                SimpleConnection simpleConnection = new SimpleClientConnection(connection);

                // note: its vital that allCreatedConnections and result contain the same SimpleConnection reference
                allCreatedConnections.add(simpleConnection);
                result.setResult(simpleConnection);
            }

            @Override
            public void failed(IOException e) {
                logger.error("Failed to establish new connection for uri {}: {}", uri, exceptionDetails(e));
                result.setException(e);
            }
        };

        UndertowClient undertowClient = UndertowClient.getInstance();
        undertowClient.connect(connectionCallback, bindAddress, uri, worker, ssl, BUFFER_POOL, connectionOptions);

        IoFuture<SimpleConnection> future = result.getIoFuture();
        return safeConnect(createConnectionTimeout, future);
    }

    @Override
    public SimpleConnection makeConnection(long createConnectionTimeout, InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, XnioSsl ssl, ByteBufferPool bufferPool, OptionMap options, final Set<SimpleConnection> allCreatedConnections) {

        final FutureResult<SimpleConnection> result = new FutureResult<>();
        ClientCallback<ClientConnection> connectionCallback = new ClientCallback<ClientConnection>() {
            @Override
            public void completed(ClientConnection connection) {
                logger.debug("New connection {} established with {}", port(connection), uri);
                SimpleConnection simpleConnection = new SimpleClientConnection(connection);

                // note: its vital that allCreatedConnections and result contain the same SimpleConnection reference
                allCreatedConnections.add(simpleConnection);
                result.setResult(simpleConnection);
            }

            @Override
            public void failed(IOException e) {
                logger.debug("Failed to establish new connection for uri: {}", uri);
                result.setException(e);
            }
        };

        Http2Client http2Client = Http2Client.getInstance();
        http2Client.connect(connectionCallback, bindAddress, uri, worker, ssl, bufferPool, options);

        IoFuture<SimpleConnection> future = result.getIoFuture();
        return safeConnect(createConnectionTimeout, future);
    }

    public SimpleConnection reuseConnection(SimpleConnection connection) throws RuntimeException
    {
        if(connection == null)
            return null;

        if(!(connection.getRawConnection() instanceof ClientConnection))
            throw new IllegalArgumentException("Attempt to reuse wrong connection type. Must be of type ClientConnection");

        if(!connection.isOpen())
            throw new RuntimeException("Reused-connection has been unexpectedly closed");

        return connection;
    }

    // PRIVATE METHODS

    private static OptionMap getConnectionOptions(boolean isHttp2) {
        return isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY;
    }

    // TODO: Should worker be re-used? Note: Light-4J Http2Client re-uses it
    private static AtomicReference<XnioWorker> WORKER = new AtomicReference<>(null);
    private static XnioWorker getWorker()
    {
        if(WORKER.get() != null) return WORKER.get();

        synchronized (SimpleClientConnectionMaker.class) {
            if(WORKER.get() != null) return WORKER.get();

            Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
            try {
                WORKER.set(xnio.createWorker(null, getWorkerOptionMap()));
            } catch (IOException e) {
                logger.error("Exception while creating new shared XnioWorker used to create connections", e);
                throw new RuntimeException(e);
            }
        }
        return WORKER.get();
    }

    private static OptionMap getWorkerOptionMap()
    {
        OptionMap.Builder optionBuild = OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, DEFAULT_WORKER_IO_THREADS)
                .set(Options.TCP_NODELAY, true)
                .set(Options.KEEP_ALIVE, true)
                .set(Options.WORKER_NAME, "simplepool");
        return  optionBuild.getMap();
    }

    // TODO: Should SSL be re-used? Note: Light-4J Http2Client re-uses it
    private static AtomicReference<UndertowXnioSsl> SSL = new AtomicReference<>(null);
    private static XnioSsl getSSL(boolean isHttps)
    {
        if(!isHttps)
            return null;
        if(SSL.get() != null)
            return SSL.get();

        synchronized (SimpleClientConnectionMaker.class) {
            if(SSL.get() != null)
                return SSL.get();

            try {
                SSL.set(new UndertowXnioSsl(getWorker().getXnio(), OptionMap.EMPTY, BUFFER_POOL, Http2Client.createSSLContext()));
            } catch (Exception e) {
                logger.error("Exception while creating new shared UndertowXnioSsl used to create connections", e);
                throw new RuntimeException(e);
            }
        }
        return SSL.get();
    }

    /***
     * Never returns null
     *
     * @param timeoutSeconds connection timeout in seconds
     * @param future contains future response containing new connection
     * @return the new Undertow connection wrapped in a SimpleConnection
     * @throws RuntimeException if connection fails
     */
    private static SimpleConnection safeConnect(long timeoutSeconds, IoFuture<SimpleConnection> future) throws RuntimeException
    {
        switch(future.await(timeoutSeconds, TimeUnit.SECONDS)) {
            case DONE:
                break;
            case WAITING:
                throw new SimplePoolConnectionFailureException("Connection establishment timed out after " + timeoutSeconds + " second(s)");
            case FAILED:
                Exception e = future.getException();
                throw new SimplePoolConnectionFailureException("Connection establishment failed: " + exceptionDetails(e), e);
            default:
                throw new SimplePoolConnectionFailureException("Connection establishment failed");
        }

        SimpleConnection connection = null;

        try {
            connection = future.get();
        } catch (IOException e) {
            throw new SimplePoolConnectionFailureException("Connection establishment generated I/O exception: " + exceptionDetails(e), e);
        }

        if(connection == null)
            throw new SimplePoolConnectionFailureException("Connection establishment failed (null) - Full connection terminated");

        return connection;
    }

    /***
     * Handles empty Exception messages for printing in logs (to avoid having "null" in logs for empty Exception messages)
     *
     * @param e Exception to look for a detail message in
     * @return the Exception message, or "" if the Exception does not contain a message
     */
    public static String exceptionDetails(Exception e) {
        if(e == null || e.getMessage() == null)
            return "";
        else
            return e.getMessage();
    }

    public static String port(ClientConnection connection) {
        if(connection == null) return "NULL";
        String url = connection.getLocalAddress().toString();
        int semiColon = url.lastIndexOf(":");
        if(semiColon == - 1) return "PORT?";
        return url.substring(url.lastIndexOf(":")+1);
    }
}
