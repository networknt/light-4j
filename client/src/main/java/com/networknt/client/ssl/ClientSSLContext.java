package com.networknt.client.ssl;

import java.security.Provider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;

public class ClientSSLContext extends SSLContext {
	public ClientSSLContext(SSLContext delegate) {
		super(null, null, null);
	}

	
	
}
