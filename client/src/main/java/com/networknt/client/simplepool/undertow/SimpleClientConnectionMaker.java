package com.networknt.client.simplepool.undertow;

import com.networknt.client.ClientConfig;
import com.networknt.client.simplepool.SimpleConnection;
import com.networknt.client.simplepool.SimpleConnectionMaker;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SimpleClientConnectionMaker implements SimpleConnectionMaker
{
    private static final Logger logger = LoggerFactory.getLogger(SimpleClientConnectionMaker.class);
    private static final ByteBufferPool BUFFER_POOL = new DefaultByteBufferPool(true, ClientConfig.get().getBufferSize() * 1024);
    private static SimpleClientConnectionMaker simpleClientConnectionMaker = null;

    public static SimpleConnectionMaker instance() {
        if(simpleClientConnectionMaker == null)
            simpleClientConnectionMaker = new SimpleClientConnectionMaker();
        return simpleClientConnectionMaker;
    }

    @Override
    public SimpleConnection makeConnection(long timeoutSeconds, boolean isHttp2, final URI uri) throws RuntimeException
    {
        boolean isHttps = uri.getScheme().equalsIgnoreCase("https");
        XnioSsl ssl = getSSL(isHttps, isHttp2);
        XnioWorker worker = getWorker(isHttp2);
        OptionMap connectionOptions = getConnectionOptions(isHttp2);
        InetSocketAddress bindAddress = null;

        final FutureResult<ClientConnection> result = new FutureResult<>();
        ClientCallback<ClientConnection> connectionCallback = new ClientCallback<ClientConnection>() {
            @Override
            public void completed(ClientConnection r) {
                logger.debug("New Connection established with {}", uri);
                result.setResult(r);
            }

            @Override
            public void failed(IOException e) {
                logger.debug("Failed to establish new connection for uri: {}", uri);
                result.setException(e);
            }
        };

        UndertowClient undertowClient = UndertowClient.getInstance();
        undertowClient.connect(connectionCallback, bindAddress, uri, worker, ssl, BUFFER_POOL, connectionOptions);

        IoFuture<ClientConnection> future = result.getIoFuture();
        ClientConnection connection = safeConnect(timeoutSeconds, future);
        return new SimpleClientConnection(connection);
    }

    public SimpleConnection reuseConnection(long timeoutSeconds, SimpleConnection connection) throws RuntimeException
    {
        if(connection == null)
            return null;

        if(!(connection.getRawConnection() instanceof ClientConnection))
            throw new IllegalArgumentException("Attempt to reuse wrong connection type. Must be of type ClientConnection");

        ClientConnection rawConnection = (ClientConnection) connection.getRawConnection();

        if(!connection.isOpen())
            throw new RuntimeException("Reused-connection has been unexpectedly closed");

        final FutureResult<ClientConnection> result = new FutureResult<>();
        result.setResult(rawConnection);
        IoFuture<ClientConnection> future = result.getIoFuture();
        ClientConnection reusedConnection = safeConnect(timeoutSeconds, future);
        return new SimpleClientConnection(reusedConnection);
    }

    // PRIVATE METHODS

    private static OptionMap getConnectionOptions(boolean isHttp2) {
        return isHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true) : OptionMap.EMPTY;
    }

    // TODO: Should worker be re-used? Note: Light-4J Http2Client re-uses it
    private static AtomicReference<XnioWorker> WORKER = new AtomicReference<>(null);
    private static XnioWorker getWorker(boolean isHttp2)
    {
        if(WORKER.get() != null) return WORKER.get();

        Xnio xnio = Xnio.getInstance(Undertow.class.getClassLoader());
        try {
            // if WORKER is null, then set new WORKER otherwise leave existing WORKER
            WORKER.compareAndSet(null, xnio.createWorker(null, getWorkerOptionMap(isHttp2)));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return WORKER.get();
    }

    private static OptionMap getWorkerOptionMap(boolean isHttp2)
    {
        OptionMap.Builder optionBuild = OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, 8)
                .set(Options.TCP_NODELAY, true)
                .set(Options.KEEP_ALIVE, true)
                .set(Options.WORKER_NAME, isHttp2 ? "Callback-HTTP2" : "Callback-HTTP11");
        return  optionBuild.getMap();
    }

    // TODO: Should SSL be re-used? Note: Light-4J Http2Client re-uses it
    private static AtomicReference<UndertowXnioSsl> SSL = new AtomicReference<>(null);
    private static XnioSsl getSSL(boolean isHttps, boolean isHttp2)
    {
        if(!isHttps)
            return null;
        if(SSL.get() != null)
            return SSL.get();

        try {
            // TODO: Should this be OptionMap.EMPTY ??
            // if SSL is null, then set new SSL otherwise leave existing SSL
            SSL.compareAndSet(
                null,
                new UndertowXnioSsl(getWorker(isHttp2).getXnio(), OptionMap.EMPTY, BUFFER_POOL, SimpleSSLContextMaker.createSSLContext()));
        } catch (Exception e) {
            logger.error("Exception while creating new shared UndertowXnioSsl used to create connections", e);
            throw new RuntimeException(e);
        }
        return SSL.get();
    }

    /***
     * Never returns null
     *
     * @param timeoutSeconds
     * @param future
     * @return
     */
    private static ClientConnection safeConnect(long timeoutSeconds, IoFuture<ClientConnection> future)
    {
        ClientConnection connection = null;

        if(future.await(timeoutSeconds, TimeUnit.SECONDS) != org.xnio.IoFuture.Status.DONE)
            throw new RuntimeException("Connection establishment timed out");
        try {
            connection = future.get();
        } catch (IOException e) {
            throw new RuntimeException("Connection establishment generated I/O exception", e);
        }
        if(connection == null)
            throw new RuntimeException("Connection establishment failed (null) - Full connection terminated");

        return connection;
    }
}
