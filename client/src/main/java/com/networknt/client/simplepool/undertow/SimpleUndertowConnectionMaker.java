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

public class SimpleUndertowConnectionMaker implements SimpleConnectionMaker
{
    private static final Logger logger = LoggerFactory.getLogger(SimpleUndertowConnectionMaker.class);
    private static final ByteBufferPool BUFFER_POOL = new DefaultByteBufferPool(true, ClientConfig.get().getBufferSize() * 1024);
    private static final AtomicReference<XnioWorker> WORKER = new AtomicReference<>(null);
    private static final AtomicReference<UndertowXnioSsl> SSL = new AtomicReference<>(null);
    private static volatile SimpleUndertowConnectionMaker simpleUndertowConnectionMaker = null;

    private SimpleUndertowConnectionMaker() {}

    public static SimpleConnectionMaker instance() {
        if(simpleUndertowConnectionMaker == null) {
            synchronized (SimpleUndertowConnectionMaker.class) {
                if (simpleUndertowConnectionMaker == null)
                    simpleUndertowConnectionMaker = new SimpleUndertowConnectionMaker();
            }
        }
        return simpleUndertowConnectionMaker;
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
                if(logger.isDebugEnabled()) logger.debug("New connection {} established with {}", port(connection), uri);
                SimpleConnection simpleConnection = new SimpleUndertowConnection(connection);

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

    // PRIVATE METHODS

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

    private static OptionMap getConnectionOptions(boolean isHttp2) {
        return isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY;
    }

    /**
     * Creates XnioWorker to make Undertow connections
     *
     * WARNING: This is called by getSSL(). Therefore, this method must never
     *          call getSSL(), or any method that transitively calls getSSL()
     *
     * @return new XnioWorker
     */
    private static XnioWorker getWorker()
    {
        if(WORKER.get() != null) return WORKER.get();

        synchronized (SimpleUndertowConnectionMaker.class) {
            // if WORKER is set then leave existing WORKER, otherwise set new WORKER
            if(WORKER.get() != null) return WORKER.get();

            Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
            try {
                WORKER.set(xnio.createWorker(null, getWorkerOptionMap()));
            } catch (IOException e) {
                logger.error("Exception while creating new XnioWorker", e);
                throw new RuntimeException(e);
            }
        }
        return WORKER.get();
    }

    private static OptionMap getWorkerOptionMap()
    {
        OptionMap.Builder optionBuild = OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, 8)
                .set(Options.TCP_NODELAY, true)
                .set(Options.KEEP_ALIVE, true)
                .set(Options.WORKER_NAME, "simplepool");
        return  optionBuild.getMap();
    }

    /**
     * Creates SSLContext and XnioSsl
     *
     * WARNING: This calls getWorker()
     *
     * @param isHttps true if this is an HTTPS connection
     * @return new XnioSSL
     */
    private static XnioSsl getSSL(boolean isHttps)
    {
        if(!isHttps) return null;
        if(SSL.get() != null) return SSL.get();

        synchronized (SimpleUndertowConnectionMaker.class) {
            // if SSL is set then leave existing SSL, otherwise set new SSL
            if(SSL.get() != null) return SSL.get();

            try {
                SSL.set(new UndertowXnioSsl(getWorker().getXnio(), OptionMap.EMPTY, BUFFER_POOL, SimpleSSLContextMaker.createSSLContext()));
            } catch (Exception e) {
                logger.error("Exception while creating new shared UndertowXnioSsl used to create connections", e);
                throw new RuntimeException(e);
            }
        }
        return SSL.get();
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