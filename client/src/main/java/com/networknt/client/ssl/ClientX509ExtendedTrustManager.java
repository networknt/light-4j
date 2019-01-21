package com.networknt.client.ssl;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

import com.networknt.client.Http2Client;

/**
 * Customized implementation of {@link javax.net.ssl.X509ExtendedTrustManager} to support validation of server identity using given trusted names.
 * 
 * @see javax.net.ssl.X509ExtendedTrustManager
 * @author Daniel Zhao
 *
 */
public class ClientX509ExtendedTrustManager extends X509ExtendedTrustManager implements X509TrustManager {
	private final X509ExtendedTrustManager extendedTrustManager;
	
	public ClientX509ExtendedTrustManager(X509ExtendedTrustManager trustManager) {
		this.extendedTrustManager = Objects.requireNonNull(trustManager);
	}
	
	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return extendedTrustManager.getAcceptedIssuers();
	}
	
	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		checkClientTrusted(chain, authType, (Socket)null);
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		checkServerTrusted(chain, authType, (Socket)null);
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
		try {
			extendedTrustManager.checkClientTrusted(chain, authType, socket);
			
			doAdditionalCheck(chain[0]);
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
		try {
			extendedTrustManager.checkServerTrusted(chain, authType, socket);
			
			doAdditionalCheck(chain[0]);
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}		
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
		try {
			extendedTrustManager.checkClientTrusted(chain, authType, engine);
			
			doAdditionalCheck(chain[0]);
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
		try {
			extendedTrustManager.checkServerTrusted(chain, authType, engine);
			
			doAdditionalCheck(chain[0]);
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}
	}
	
	private void doAdditionalCheck(X509Certificate cert) throws CertificateException{
		APINameChecker.verifyAndThrow(Http2Client.TLS_CONFIG.getEndpointIdentificationAlgorithm(), Http2Client.TLS_CONFIG.getTrustedNameSet(), cert);
	}
}
