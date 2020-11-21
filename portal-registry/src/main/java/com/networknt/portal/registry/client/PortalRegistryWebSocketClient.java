package com.networknt.portal.registry.client;

import com.networknt.client.Http2Client;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.websockets.core.*;
import org.xnio.*;

import java.io.IOException;
import java.net.URI;

/**
 * Created by leiko on 27/02/15.
 *
 */
public abstract class PortalRegistryWebSocketClient implements WebSocketClientHandlers {

    private WebSocketChannel channel;
    private FutureNotifier futureNotifier = new FutureNotifier(this);

    /**
     *
     * @param uri web socket server uri
     * @throws IOException exception
     */
    public PortalRegistryWebSocketClient(URI uri) throws IOException {
        UndertowXnioSsl ssl = new UndertowXnioSsl(Xnio.getInstance(), OptionMap.EMPTY, Http2Client.createSSLContext());
        final io.undertow.websockets.client.WebSocketClient.ConnectionBuilder connectionBuilder = io.undertow.websockets.client.WebSocketClient.connectionBuilder(Http2Client.WORKER, Http2Client.BUFFER_POOL, uri).setSsl(ssl);
        IoFuture<WebSocketChannel> future = connectionBuilder.connect();
        future.addNotifier(futureNotifier, null);
    }

    /**
     * Close connection with remote web socket server
     * @throws IOException exception
     */
    public void close() throws IOException {
        if (this.channel != null) {
            this.channel.sendClose();
        }
    }

    /**
     *
     * @return true if currently connected to remote server
     */
    public boolean isOpen() {
        return this.channel != null && this.channel.isOpen();
    }

    /**
     *
     * @param text message to send to server
     */
    public void send(String text) {
        this.send(text, null);
    }

    /**
     *
     * @param text text string
     * @param callback called once processed
     */
    public void send(String text, WebSocketCallback<Void> callback) {
        if (this.channel != null && this.channel.isOpen()) {
            WebSockets.sendText(text, this.channel, callback);
        }
    }

    /**
     * Code readability helper
     */
    private class FutureNotifier extends IoFuture.HandlingNotifier<WebSocketChannel, Object> {

        private PortalRegistryWebSocketClient client;

        public FutureNotifier(PortalRegistryWebSocketClient client) {
            this.client = client;
        }

        @Override
        public void handleFailed(IOException exception, Object attachment) {
            this.client.onError(exception);
        }

        @Override
        public void handleDone(WebSocketChannel channel, Object attachment) {
            this.client.channel = channel;
            this.client.onOpen();

            channel.getReceiveSetter().set(new AbstractReceiveListener() {
                @Override
                protected void onFullTextMessage(WebSocketChannel ws, BufferedTextMessage message) throws IOException {
                    client.onMessage(message.getData());
                }

                @Override
                protected void onError(WebSocketChannel ws, Throwable error) {
                    super.onError(ws, error);
                    client.onError(new Exception(error));
                }
            });

            channel.resumeReceives();
            channel.addCloseTask(ws -> client.onClose(ws.getCloseCode(), ws.getCloseReason()));
        }
    }
}
