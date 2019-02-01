package com.networknt.client.ssl;

import java.security.cert.CertificateException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SSLUtils {
	private static final Logger logger = LoggerFactory.getLogger(SSLUtils.class);
	
	public static void handleTrustValidationErrors(Throwable t) throws CertificateException{
		logger.error(t.getMessage(), t);
		
		if (t instanceof CertificateException) {
			throw (CertificateException)t;
		}
		
		throw new CertificateException(t);
	}
}
