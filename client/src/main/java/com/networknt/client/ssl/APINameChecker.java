package com.networknt.client.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.apache.http.conn.ssl.DefaultHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APINameChecker {
	private static final Logger logger = LoggerFactory.getLogger(APINameChecker.class);
	private static final DefaultHostnameVerifier verifier = new DefaultHostnameVerifier();
	
	public static void verifyAndThrow(EndpointIdentificationAlgorithm identityAlg, final Set<String> nameSet, final X509Certificate cert) throws CertificateException{
		if (EndpointIdentificationAlgorithm.API==identityAlg && !verify(nameSet, cert)) {
			throw new CertificateException("No name matching " + nameSet + " found");
		}
	}
	
	public static boolean verify(final Set<String> nameSet, final X509Certificate cert)  {
		if (null!=nameSet && !nameSet.isEmpty()) {
			return nameSet.stream().filter(name->verify(name, cert)).findAny().isPresent();
		}
		
		return false;
	}

	
    public static boolean verify(final String name, final X509Certificate cert) {
        try {
        	verifier.verify(name, cert);
            return true;
        } catch (final SSLException ex) {
            if (logger.isDebugEnabled()) {
            	logger.debug(ex.getMessage(), ex);
            }
            return false;
        }
    }
}
