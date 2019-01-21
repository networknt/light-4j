package com.networknt.client.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;

import javax.net.ssl.X509TrustManager;

import com.networknt.client.Http2Client;

/**
 * Customized implementation of {@link javax.net.ssl.X509TrustManager} to support validation of server identity using given trusted names.
 * Note: X509TrustManagers are eventually converted to {@link javax.net.ssl.X509ExtendedTrustManager} by sun.security.ssl.SSLContextImpl.AbstractTrustManagerWrapper
 * 
 * @author Daniel Zhao
 *
 */
public class CientX509TrustManager implements X509TrustManager{
	private final X509TrustManager trustManager;
	
	public CientX509TrustManager(X509TrustManager trustManager) {
		this.trustManager = Objects.requireNonNull(trustManager);
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		try {
			trustManager.checkClientTrusted(chain, authType);
			
			doAdditionalCheck(chain[0]);
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}		
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
		try {
			trustManager.checkServerTrusted(chain, authType);
			
			doAdditionalCheck(chain[0]);
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}			
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return trustManager.getAcceptedIssuers();
	}
	
	private void doAdditionalCheck(X509Certificate cert) throws CertificateException{
		APINameChecker.verifyAndThrow(Http2Client.TLS_CONFIG.getEndpointIdentificationAlgorithm(), Http2Client.TLS_CONFIG.getTrustedNameSet(), cert);
	}
}
