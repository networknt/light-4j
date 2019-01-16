package com.networknt.client.ssl;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.ChannelListeners;
import org.xnio.IoFuture;
import org.xnio.OptionMap;
import org.xnio.Xnio;
import org.xnio.XnioIoThread;
import org.xnio.XnioWorker;
import org.xnio.channels.BoundChannel;
import org.xnio.ssl.JsseSslUtils;
import org.xnio.ssl.SslConnection;

import com.networknt.client.Http2Client;
import com.networknt.config.Config;
import com.networknt.utility.StringUtils;

import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.SslConduit;
import io.undertow.protocols.ssl.UndertowXnioSsl;

/**
 * This class is created to add necessary SSL parameters and retrieve SSL handshake results.
 * 
 * @author Daniel Zhao
 *
 */
public class Light4jXnioSsl extends UndertowXnioSsl {
	private static final Logger logger = LoggerFactory.getLogger(Light4jXnioSsl.class);
	// not using ThreadLocal because executors are used. And also because the handshakeMonitor are used across threads.
	private static final Map<String, CompletableFuture<Boolean>> handshakeMonitors = new ConcurrentHashMap<>();
	private static final String DELIMITER="|";
	private static final AtomicLong ID_SEQUENCE = new AtomicLong(0);
	
	
	private static final Map<String, Object> CLIENT_CONFIG = Config.getInstance().getJsonMapConfig(Http2Client.CONFIG_NAME);
	private static final long DEFAULT_HANDSHARE_TIMEOUT = 1000;
	public static final String HANDSHARE_TIMEOUT = "handshareTimeout";
	
    public Light4jXnioSsl(final Xnio xnio, final OptionMap optionMap) throws NoSuchProviderException, NoSuchAlgorithmException, KeyManagementException {
    	//call super so that we don't need to redefine the private constant DEFAULT_BUFFER_POOL
        super(xnio, optionMap);
    }

    public Light4jXnioSsl(final Xnio xnio, final OptionMap optionMap, final SSLContext sslContext) {
    	//call super so that we don't need to redefine the private constant DEFAULT_BUFFER_POOL
    	super(xnio, optionMap, sslContext);
    }

    public Light4jXnioSsl(final Xnio xnio, final OptionMap optionMap, ByteBufferPool bufferPool) throws NoSuchProviderException, NoSuchAlgorithmException, KeyManagementException {
        this(xnio, optionMap, bufferPool, JsseSslUtils.createSSLContext(optionMap));
    }	

	public Light4jXnioSsl(final Xnio xnio, final OptionMap optionMap, ByteBufferPool bufferPool, final SSLContext sslContext) {
		super(xnio, optionMap, bufferPool, sslContext);
	}
	
	/**
	 * Override the parent implementation to add customized openListener
	 * 
	 * {@inheritDoc}
	 */
	@Override
    public IoFuture<SslConnection> openSslConnection(final XnioWorker worker, final InetSocketAddress bindAddress, final InetSocketAddress destination, final ChannelListener<? super SslConnection> openListener, final ChannelListener<? super BoundChannel> bindListener, final OptionMap optionMap) {
		return super.openSslConnection(worker, bindAddress, destination, new SslConnectionListener(getHandshakeMonitor(optionMap), openListener), bindListener, optionMap);
    }
	
	/**
	 * Override the parent implementation to add customized openListener
	 * 
	 * {@inheritDoc}
	 */
    @Override
    public IoFuture<SslConnection> openSslConnection(final XnioIoThread ioThread, final InetSocketAddress bindAddress, final InetSocketAddress destination, final ChannelListener<? super SslConnection> openListener, final ChannelListener<? super BoundChannel> bindListener, final OptionMap optionMap) {
    	return super.openSslConnection(ioThread, bindAddress, destination, new SslConnectionListener(getHandshakeMonitor(optionMap), openListener), bindListener, optionMap);
    }
    
    public String createConnectionId(final URI uri) {
    	return new StringBuilder().append(uri.toString())
    	    	.append(DELIMITER)
    	    	.append(ID_SEQUENCE.getAndIncrement()).toString();
    }
    
    public void createHandshakeMonitor(final String connectionId) {
    	handshakeMonitors.put(connectionId, new CompletableFuture<>());
    }
    
    private CompletableFuture<Boolean> getHandshakeMonitor(OptionMap optionMap){
    	String connectionId = optionMap.get(Http2Client.CONNECTION_ID);
    	
    	if (StringUtils.isNotBlank(connectionId)) {
    		return handshakeMonitors.get(connectionId);
    	}
    	
    	return null;
    }
    
    public boolean isHandshakeDown(String connectionId) {
    	boolean result = false;
    	
    	CompletableFuture<Boolean> handshakeMonitor = handshakeMonitors.get(connectionId);
    	
    	long timeout = (Long)CLIENT_CONFIG.getOrDefault(HANDSHARE_TIMEOUT, DEFAULT_HANDSHARE_TIMEOUT);
    	
    	try {
    		result = handshakeMonitor.get(timeout, TimeUnit.MILLISECONDS);
    	} catch (Throwable t) {
    		logger.error("cannot get handshare result");
    	}
    	
    	return result;
    }
    
    /**
     * Setup SSL parameters before handshake
     * 
     * Note: SslConnection is only available in openListener.
     * 
     * @see io.undertow.protocols.ssl.UndertowXnioSsl.StreamConnectionChannelListener
     * 
     * @param openListener
     * @return
     */
	private class SslConnectionListener implements ChannelListener<SslConnection> {
		private final CompletableFuture<Boolean> handshakeMonitor;
		private final ChannelListener<? super SslConnection> openListener;

		SslConnectionListener(CompletableFuture<Boolean> handshakeMonitor, ChannelListener<? super SslConnection> openListener) {
			this.handshakeMonitor = handshakeMonitor;
			this.openListener = openListener;
		}

		@Override
		public void handleEvent(SslConnection connection) {
			if (logger.isDebugEnabled()) {
				logger.debug("adapting open listener...");
			}
			
			SSLEngine engine = getSslEngine(connection);

			EndpointIdentificationAlgorithm.setup(engine, EndpointIdentificationAlgorithm.API);

			ChannelListeners.invokeChannelListener(connection, openListener);
			
			if (null!=handshakeMonitor) {
				connection.getHandshakeSetter().set(new ChannelListener<SslConnection>() {
					@Override
					public void handleEvent(SslConnection connection) {
						if (logger.isDebugEnabled()) {
							logger.debug("------ handshake is done ...");
						}
						
						handshakeMonitor.complete(true);
					}
				});

				connection.getCloseSetter().set(new ChannelListener<SslConnection>() {
					@Override
					public void handleEvent(SslConnection connection) {
						if (logger.isDebugEnabled()) {
							logger.debug("------ conn is closed ...");
						}
						
						handshakeMonitor.complete(false);
					}
				});	
			}
		}
	}
    
}
