package com.networknt.client.ssl;

import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

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

import io.undertow.connector.ByteBufferPool;
import io.undertow.protocols.ssl.SslConduit;
import io.undertow.protocols.ssl.UndertowXnioSsl;

public class Light4jXnioSsl extends UndertowXnioSsl {
	private static final Logger logger = LoggerFactory.getLogger(Light4jXnioSsl.class);
	
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
        return super.openSslConnection(worker, bindAddress, destination, adaptOpenListener(openListener),bindListener, optionMap);
    }
	
	/**
	 * Override the parent implementation to add customized openListener
	 * 
	 * {@inheritDoc}
	 */
    @Override
    public IoFuture<SslConnection> openSslConnection(final XnioIoThread ioThread, final InetSocketAddress bindAddress, final InetSocketAddress destination, final ChannelListener<? super SslConnection> openListener, final ChannelListener<? super BoundChannel> bindListener, final OptionMap optionMap) {
        return super.openSslConnection(ioThread, bindAddress, destination, adaptOpenListener(openListener), bindListener, optionMap);
    }
    
    /**
     * Setup SSL parameters before handshake
     * 
     * @param openListener
     * @return
     */
    protected ChannelListener<? super SslConnection> adaptOpenListener(ChannelListener<? super SslConnection> openListener) {
        return new ChannelListener<SslConnection>() {
            @Override
            public void handleEvent(SslConnection connection) {
            	if (logger.isDebugEnabled()) {
            		logger.debug("adapting open listener...");
            	}
            	
            	
            	SSLEngine engine = getSslEngine(connection);
            	
            	SslConduit conduit = getSslConduit(connection);
            	
            	EndpointIdentificationAlgorithm.setup(engine, EndpointIdentificationAlgorithm.HTTPS);
            	
            	ChannelListeners.invokeChannelListener(connection, openListener);
            	
            	connection.getCloseSetter().set(new ChannelListener<SslConnection>() {
                    @Override
                    public void handleEvent(SslConnection connection) {
                    	if (logger.isDebugEnabled()) {
                    		logger.debug("------ conn is closed ...");
                    	}
                    }
            	});            	
            	
            	
            	
            }
        };
    }
}
