package io.undertow.client.http;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.xnio.ChannelListener;
import org.xnio.IoFuture;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.StreamConnection;
import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;
import org.xnio.ssl.SslConnection;
import org.xnio.ssl.XnioSsl;

import com.networknt.client.ssl.Light4jALPNClientSelector;

import io.undertow.UndertowLogger;
import io.undertow.UndertowMessages;
import io.undertow.UndertowOptions;
import io.undertow.client.ALPNClientSelector;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientProvider;
import io.undertow.client.ClientStatistics;
import io.undertow.client.http2.Http2ClientConnection;
import io.undertow.conduits.ByteActivityCallback;
import io.undertow.conduits.BytesReceivedStreamSourceConduit;
import io.undertow.conduits.BytesSentStreamSinkConduit;
import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.http2.Http2Channel;

/**
 * Customized HttpClientProvider for handling TLS handshake for HTTP2.
 * Created by modifying {@link io.undertow.client.http2.Http2ClientProvider}
 * 
 *
 */
public class Light4jHttp2ClientProvider implements ClientProvider {

    public static final String HTTP2 = "h2";
    public static final String HTTP_1_1 = "http/1.1";

    private static final ChannelListener<SslConnection> FAILED = new ChannelListener<SslConnection>() {
        @Override
        public void handleEvent(SslConnection connection) {
            UndertowLogger.ROOT_LOGGER.alpnConnectionFailed(connection);
            IoUtils.safeClose(connection);
        }
    };

    @Override
    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioWorker worker, final XnioSsl ssl, final ByteBufferPool bufferPool, final OptionMap options) {
        connect(listener, null, uri, worker, ssl, bufferPool, options);
    }

    @Override
    public void connect(final ClientCallback<ClientConnection> listener, final URI uri, final XnioIoThread ioThread, final XnioSsl ssl, final ByteBufferPool bufferPool, final OptionMap options) {
        connect(listener, null, uri, ioThread, ssl, bufferPool, options);
    }

    @Override
    public Set<String> handlesSchemes() {
        return new HashSet<>(Arrays.asList(new String[]{HTTP2}));
    }

    @Override
    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioWorker worker, final XnioSsl ssl, final ByteBufferPool bufferPool, final OptionMap options) {
        if (ssl == null) {
            listener.failed(UndertowMessages.MESSAGES.sslWasNull());
            return;
        }
        OptionMap tlsOptions = OptionMap.builder().addAll(options).set(Options.SSL_STARTTLS, true).getMap();
        if(bindAddress == null) {
            ssl.openSslConnection(worker, new InetSocketAddress(uri.getHost(), uri.getPort() == -1 ? 443 : uri.getPort()), createOpenListener(listener, uri, ssl, bufferPool, tlsOptions), tlsOptions).addNotifier(createNotifier(listener), null);
        } else {
            ssl.openSslConnection(worker, bindAddress, new InetSocketAddress(uri.getHost(), uri.getPort() == -1 ? 443 : uri.getPort()), createOpenListener(listener, uri, ssl, bufferPool, tlsOptions), tlsOptions).addNotifier(createNotifier(listener), null);
        }

    }

    @Override
    public void connect(final ClientCallback<ClientConnection> listener, InetSocketAddress bindAddress, final URI uri, final XnioIoThread ioThread, final XnioSsl ssl, final ByteBufferPool bufferPool, final OptionMap options) {
        if (ssl == null) {
            listener.failed(UndertowMessages.MESSAGES.sslWasNull());
            return;
        }
        if(bindAddress == null) {
            OptionMap tlsOptions = OptionMap.builder().addAll(options).set(Options.SSL_STARTTLS, true).getMap();
            ssl.openSslConnection(ioThread, new InetSocketAddress(uri.getHost(), uri.getPort() == -1 ? 443 : uri.getPort()), createOpenListener(listener, uri, ssl, bufferPool, tlsOptions), options).addNotifier(createNotifier(listener), null);
        } else {
            ssl.openSslConnection(ioThread, bindAddress, new InetSocketAddress(uri.getHost(), uri.getPort() == -1 ? 443 : uri.getPort()), createOpenListener(listener, uri, ssl, bufferPool, options), options).addNotifier(createNotifier(listener), null);
        }

    }

    protected IoFuture.Notifier<StreamConnection, Object> createNotifier(final ClientCallback<ClientConnection> listener) {
        return new IoFuture.Notifier<StreamConnection, Object>() {
            @Override
            public void notify(IoFuture<? extends StreamConnection> ioFuture, Object o) {
                if (ioFuture.getStatus() == IoFuture.Status.FAILED) {
                    listener.failed(ioFuture.getException());
                }
            }
        };
    }

    protected ChannelListener<StreamConnection> createOpenListener(final ClientCallback<ClientConnection> listener, final URI uri, final XnioSsl ssl, final ByteBufferPool bufferPool, final OptionMap options) {
        return new ChannelListener<StreamConnection>() {
            @Override
            public void handleEvent(StreamConnection connection) {
                handleConnected(connection, listener, uri, bufferPool, options);
            }
        };
    }

    public static ALPNClientSelector.ALPNProtocol alpnProtocol(final ClientCallback<ClientConnection> listener, URI uri, ByteBufferPool bufferPool, OptionMap options) {
        return new ALPNClientSelector.ALPNProtocol(new ChannelListener<SslConnection>() {
            @Override
            public void handleEvent(SslConnection connection) {
                listener.completed(createHttp2Channel(connection, bufferPool, options, uri.getHost()));
            }
        }, HTTP2);
    };

    protected void handleConnected(StreamConnection connection, final ClientCallback<ClientConnection> listener, URI uri,ByteBufferPool bufferPool, OptionMap options) {
    	Light4jALPNClientSelector.runAlpn((SslConnection) connection, FAILED, listener, alpnProtocol(listener, uri, bufferPool, options));
    }

    protected static Http2ClientConnection createHttp2Channel(StreamConnection connection, ByteBufferPool bufferPool, OptionMap options, String defaultHost) {

        final ClientStatisticsImpl clientStatistics;
        //first we set up statistics, if required
        if (options.get(UndertowOptions.ENABLE_STATISTICS, false)) {
            clientStatistics = new ClientStatisticsImpl();
            connection.getSinkChannel().setConduit(new BytesSentStreamSinkConduit(connection.getSinkChannel().getConduit(), new ByteActivityCallback() {
                @Override
                public void activity(long bytes) {
                    clientStatistics.written += bytes;
                }
            }));
            connection.getSourceChannel().setConduit(new BytesReceivedStreamSourceConduit(connection.getSourceChannel().getConduit(), new ByteActivityCallback() {
                @Override
                public void activity(long bytes) {
                    clientStatistics.read += bytes;
                }
            }));
        } else {
            clientStatistics = null;
        }
        Http2Channel http2Channel = new Http2Channel(connection, null, bufferPool, null, true, false, options);
        return new Http2ClientConnection(http2Channel, false, defaultHost, clientStatistics, true);
    }

    protected static class ClientStatisticsImpl implements ClientStatistics {
        private long requestCount, read, written;
        @Override
        public long getRequests() {
            return requestCount;
        }

        @Override
        public long getRead() {
            return read;
        }

        @Override
        public long getWritten() {
            return written;
        }

        @Override
        public void reset() {
            read = 0;
            written = 0;
            requestCount = 0;
        }
    } 
}
