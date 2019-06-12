/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.client.ssl;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Set;

import javax.net.ssl.SSLException;

import org.apache.hc.client5.http.ssl.copied.DefaultHostnameVerifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APINameChecker {
	private static final Logger logger = LoggerFactory.getLogger(APINameChecker.class);
	private static final DefaultHostnameVerifier verifier = new DefaultHostnameVerifier();
	
	
	/**
	 * Perform server identify check using given names and throw CertificateException if the check fails.
	 * 
	 * @param nameSet - a set of names from the config file
	 * @param cert - the server certificate
	 * @throws CertificateException - throws CertificateException if none of the name in the nameSet matches the names in the cert
	 */
	public static void verifyAndThrow(final Set<String> nameSet, final X509Certificate cert) throws CertificateException{
		if (!verify(nameSet, cert)) {
			throw new CertificateException("No name matching " + nameSet + " found");
		}
	}
	
	/**
	 * Perform server identify check using given name and throw CertificateException if the check fails.
	 * 
	 * @param name string
	 * @param cert X509Certificate
	 * @throws CertificateException CertificateException
	 */
	public static void verifyAndThrow(final String name, final X509Certificate cert) throws CertificateException{
		if (!verify(name, cert)) {
			throw new CertificateException("No name matching " + name + " found");
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
