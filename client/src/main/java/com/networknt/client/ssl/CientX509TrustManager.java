package com.networknt.client.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CientX509TrustManager implements X509TrustManager{
	private static final Logger logger = LoggerFactory.getLogger(CientX509TrustManager.class);
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
	public void checkClientTrusted(X509Certificate[] var1, String var2) throws CertificateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void checkServerTrusted(X509Certificate[] var1, String var2) throws CertificateException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return trustManager.getAcceptedIssuers();
	}
}
