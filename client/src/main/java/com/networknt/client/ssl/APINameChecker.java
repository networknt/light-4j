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
	
	
	/**
	 * Perform server identify check using given names and throw CertificateException if the check fails.
	 * This method is only used for the custom algorithm EndpointIdentificationAlgorithm.APIS.
	 * The validation of standard algorithms are performed by trustmanagers shipped with the JRE.
	 * 
	 * @param identityAlg - The EndpointIdentificationAlgorithm parsed from the config file.
	 * @param nameSet - a set of names from the config file
	 * @param cert - the server certificate
	 * @throws CertificateException - throws CertificateException if none of the name in the nameSet matches the names in the cert
	 */
	public static void verifyAndThrow(EndpointIdentificationAlgorithm identityAlg, final Set<String> nameSet, final X509Certificate cert) throws CertificateException{
		if (EndpointIdentificationAlgorithm.APIS==identityAlg && !verify(nameSet, cert)) {
			throw new CertificateException("No name matching " + nameSet + " found");
		}
	}
	
	/**
	 * Verify a set of names as per HTTPS (RFC 2818)
	 * 
	 * @param nameSet - a set of names from the config file
	 * @param cert - the server certificate
	 * @return - whether any name in the name set matches names in the cert
	 */
	public static boolean verify(final Set<String> nameSet, final X509Certificate cert)  {
		if (null!=nameSet && !nameSet.isEmpty()) {
			return nameSet.stream().filter(name->verify(name, cert)).findAny().isPresent();
		}
		
		return false;
	}
	
	/**
	 * Call DefaultHostnameVerifier to check hostnames as per HTTPS (RFC 2818)
	 * 
	 * @param name - a name
	 * @param cert - the server certificate
	 * @return - whether the given name matches names in the cert
	 */
    public static boolean verify(final String name, final X509Certificate cert) {
        try {
        	verifier.verify(name, cert);
            return true;
        } catch (final SSLException ex) {
        	// this is only logged here because eventually a CertificateException will be throw in verifyAndThrow.
        	// If this method is called in another method, the caller should be responsible to throw exceptions,
            logger.error(ex.getMessage(), ex);
            
            return false;
        }
    }
}
