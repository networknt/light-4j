package com.networknt.client.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

/**
 * Customized X509TrustManager. 
 * Note: X509TrustManagers are eventually converted to X509ExtendedTrustManagers by sun.security.ssl.SSLContextImpl.AbstractTrustManagerWrapper
 * 
 * @author Daniel Zhao
 *
 */
public class CientX509TrustManager implements X509TrustManager{
	private final X509TrustManager trustManager;
	private final Set<String> trustedNameSet;
	private final EndpointIdentificationAlgorithm identityAlg;
	
	public CientX509TrustManager(X509TrustManager trustManager) {
		this(trustManager, false, null);
	}
	
	public CientX509TrustManager(X509TrustManager trustManager, boolean checkIdentity, String trustedNames) {
		this.trustManager = Objects.requireNonNull(trustManager);
		trustedNameSet = SSLUtils.resolveTrustedNames(trustedNames);
		identityAlg = EndpointIdentificationAlgorithm.select(checkIdentity, trustedNameSet);
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
		APINameChecker.verifyAndThrow(identityAlg, trustedNameSet, cert);
	}
}
