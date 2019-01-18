package com.networknt.client.ssl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ssl.SSLEngine;

import org.xnio.ChannelListener;
import org.xnio.channels.StreamSourceChannel;
import org.xnio.conduits.PushBackStreamSourceConduit;
import org.xnio.ssl.SslConnection;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;

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
	@SuppressWarnings("unchecked")
	private static final Map<String, Object> TLS_CONFIG = (Map<String, Object>)Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME).get(Http2Client.TLS);
	
    public static void runAlpn(final SslConnection sslConnection, final ChannelListener<SslConnection> fallback, final ClientCallback<ClientConnection> failedListener, final ALPNProtocol... details) {
        SslConduit conduit = UndertowXnioSsl.getSslConduit(sslConnection);
        SSLEngine engine = UndertowXnioSsl.getSslEngine(sslConnection);
        
    	// set ssl parameters
        Set<String> trustedNameSet = SSLUtils.resolveTrustedNames((String)TLS_CONFIG.get(Http2Client.TRUSTED_NAMES));
        EndpointIdentificationAlgorithm alg = EndpointIdentificationAlgorithm.select((Boolean)TLS_CONFIG.get(Http2Client.VERIFY_HOSTNAME), trustedNameSet);
    	EndpointIdentificationAlgorithm.setup(engine, alg);

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
                        } else {// modification of ALPNClientSelector for JDK8. need to check for handshake results.
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
