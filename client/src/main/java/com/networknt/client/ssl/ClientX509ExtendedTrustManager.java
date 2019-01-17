package com.networknt.client.ssl;

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This trust manager is created due to the fact that the identity check is not enabled in X509TrustManager by default.
 * There are two approaches to enable the identity check:
 * 1. Customize SSLContext, SSLContextSpi, and SSLSocketFactory to set EndpointIdentificationAlgorithm for SSLEngine and SSLSocket once they are created.
 * 2. Customize the X509TrustManager to set EndpointIdentificationAlgorithm for SSLEngine and SSLSocket before the validation is started.
 * 
 * We prefer approach 2 over approach 1 for two reasons:
 * a. Approach 1 requires more changes, including 3 customized classes and tens of methods. With approach 2, all required changes are in this class.
 * b. Approach 1 is not easy to understand or debug since it's not obvious why the algorithm needs to be set. With approach 2, the algorithm is set right before where it is used.
 * 
 * In addition, this class also adds logs for validation failures (i.e, CertificateException). These failures are swallowed within a multi-thread context (e.g., undertow).
 * 
 * Note: Trust manager needs to be an instance of X509ExtendedTrustManager in order to accomplish identify check. 
 * 
 * @see javax.net.ssl.X509ExtendedTrustManager
 * @author Daniel Zhao
 *
 */
public class ClientX509ExtendedTrustManager extends X509ExtendedTrustManager implements X509TrustManager {
	private final X509ExtendedTrustManager extendedTrustManager;
	private final Set<String> trustedNameSet;
	private final EndpointIdentificationAlgorithm identityAlg;
	
	public ClientX509ExtendedTrustManager(X509ExtendedTrustManager trustManager) {
		this(trustManager, false, null);
	}
	
	public ClientX509ExtendedTrustManager(X509ExtendedTrustManager trustManager, boolean checkIdentity, String trustedNames) {
		this.extendedTrustManager = Objects.requireNonNull(trustManager);
		trustedNameSet = SSLUtils.resolveTrustedNames(trustedNames);
		identityAlg = EndpointIdentificationAlgorithm.select(checkIdentity, trustedNameSet);
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
		APINameChecker.verifyAndThrow(identityAlg, trustedNameSet, cert);
	}
}
