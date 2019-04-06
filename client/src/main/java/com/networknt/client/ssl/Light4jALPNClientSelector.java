/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.networknt.client.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLEngine;

import org.xnio.ChannelListener;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.conduits.PushBackStreamSourceConduit;
import org.xnio.ssl.SslConnection;

import io.undertow.client.ALPNClientSelector;
import io.undertow.client.ALPNClientSelector.ALPNProtocol;
import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.protocols.alpn.ALPNManager;
import io.undertow.protocols.alpn.ALPNProvider;
import io.undertow.protocols.ssl.SslConduit;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.util.ImmediatePooled;

/**
 * Customized ALPNClientSelector for handling TLS handshake for HTTP2.
 * 
 * @author Daniel Zhao
 *
 */
public class Light4jALPNClientSelector {
	
	/**
	 * A connection is first created by org.xnio.nio.WorkerThread.openTcpStreamConnection(). Once the connection is established, it is passed to io.undertow.protocols.ssl.UndertowXnioSsl.StreamConnectionChannelListener.
	 * StreamConnectionChannelListener creates an io.undertow.protocols.ssl.UndertowSslConnection instance and passes it to this method.
	 * 
	 * This method uses the provided sslConnection to perform Application-Layer Protocol Negotiation (ALPN). More specifically, this method negotiates with the server side about which protocol should be used.
	 *  - If the negotiation succeeds, the selected protocol is passed to the corresponding channel listeners defined in the 'details' argument. 
	 *    For example, if HttpClientProvider is used and http2 is selected in the negotiation result, Http2ClientConnection will be created.
	 *  - If the negotiation fails (i.e., selectedProtocol is null), the fallback listener is used to continue the communication if possible or simply close the connection.
	 *    For the example above, if http2 is not supported on the server side, HttpClientConnection will be created in the fallback listener.
	 * 
	 * @param sslConnection - an UndertowSslConnection instance
	 * @param fallback - the callback used if the ALPN negotiation fails or no APLN provider can be found
	 * @param failedListener - the callback for handling failures happened in the negotiations
	 * @param details - callbacks used to create client connections when the negotiation succeeds. Ideally, one callback should be provided for each protocol in {@link javax.net.ssl.SSLEngine#getSupportedProtocols()}.
	 */
    public static void runAlpn(final SslConnection sslConnection, final ChannelListener<SslConnection> fallback, final ClientCallback<ClientConnection> failedListener, final ALPNProtocol... details) {
        SslConduit conduit = UndertowXnioSsl.getSslConduit(sslConnection);

        final ALPNProvider provider = ALPNManager.INSTANCE.getProvider(conduit.getSSLEngine());
        if (provider == null) {
            fallback.handleEvent(sslConnection);
            return;
        }
        String[] protocols = new String[details.length];
        final Map<String, ALPNProtocol> protocolMap = new HashMap<>();
        for (int i = 0; i < protocols.length; ++i) {
            protocols[i] = details[i].getProtocol();
            protocolMap.put(details[i].getProtocol(), details[i]);
        }
        final SSLEngine sslEngine = provider.setProtocols(conduit.getSSLEngine(), protocols);
        conduit.setSslEngine(sslEngine);
        final AtomicReference<Boolean> handshakeDone = new AtomicReference<>(false);
        final AtomicReference<Boolean> connClosed = new AtomicReference<>(false);

        try {
            sslConnection.getHandshakeSetter().set(new ChannelListener<SslConnection>() {
                @Override
                public void handleEvent(SslConnection channel) {
                    if(handshakeDone.get()) {
                        return;
                    }
                    handshakeDone.set(true);
                }
            });
            
            sslConnection.getCloseSetter().set(new ChannelListener<SslConnection>() {
                @Override
                public void handleEvent(SslConnection channel) {
                    if(connClosed.get()) {
                        return;
                    }
                    connClosed.set(true);
                }
            });
            
            sslConnection.startHandshake();
            
            sslConnection.getSourceChannel().getReadSetter().set(new ChannelListener<StreamSourceChannel>() {
                @Override
                public void handleEvent(StreamSourceChannel channel) {

                    String selectedProtocol = provider.getSelectedProtocol(sslEngine);
                    if (selectedProtocol != null) {
                        handleSelected(selectedProtocol);
                    } else {
                        ByteBuffer buf = ByteBuffer.allocate(100);
                        try {
                            int read = channel.read(buf);
                            if (read > 0) {
                                buf.flip();
                                PushBackStreamSourceConduit pb = new PushBackStreamSourceConduit(sslConnection.getSourceChannel().getConduit());
                                pb.pushBack(new ImmediatePooled<>(buf));
                                sslConnection.getSourceChannel().setConduit(pb);
                            } else if (read == -1) {
                                failedListener.failed(new ClosedChannelException());
                            }
                            selectedProtocol = provider.getSelectedProtocol(sslEngine);
                            if (selectedProtocol != null) {
                                handleSelected(selectedProtocol);
                            } else if (read > 0 || handshakeDone.get()) {
                                sslConnection.getSourceChannel().suspendReads();
                                fallback.handleEvent(sslConnection);
                                return;
                            }
                        } catch (Throwable t) {
                            IOException e = t instanceof IOException ? (IOException) t : new IOException(t);
                            failedListener.failed(e);
                        }
                    }
                }

                private void handleSelected(String selected) {
                    if (selected.isEmpty()) {
                        sslConnection.getSourceChannel().suspendReads();
                        fallback.handleEvent(sslConnection);
                        return;
                    } else {
                        ALPNClientSelector.ALPNProtocol details = protocolMap.get(selected);
                        if (details == null) {
                            //should never happen
                            sslConnection.getSourceChannel().suspendReads();
                            fallback.handleEvent(sslConnection);
                            return;
                        } else {// modification of ALPNClientSelector for JDK8. need to check handshake results.
                        	if (handshakeDone.get()) {
                                sslConnection.getSourceChannel().suspendReads();
                                details.getSelected().handleEvent(sslConnection);                        		
                        	}else if (connClosed.get()) {
                        		failedListener.failed(new ClosedChannelException());
                        	}
                        }
                    }
                }
            });
            sslConnection.getSourceChannel().resumeReads();
        } catch (IOException e) {
            failedListener.failed(e);
        } catch (Throwable e) {
            failedListener.failed(new IOException(e));
        }

    }	
}
