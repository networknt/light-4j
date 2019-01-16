package com.networknt.client.ssl;

import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.networknt.utility.StringUtils;

public class SSLUtils {
	private static final Logger logger = LoggerFactory.getLogger(SSLUtils.class);
	
	public static Set<String> resolveTrustedNames(String trustedNames){
		Set<String> nameSet = Arrays.stream(StringUtils.trimToEmpty(trustedNames).split(","))
				.filter(StringUtils::isNotBlank)
				.collect(Collectors.toSet());
		
		if (logger.isDebugEnabled()) {
			logger.debug("trusted names {}", nameSet);
		}
		
		return nameSet;
	}
	
	public static void handleTrustValidationErrors(Throwable t) throws CertificateException{
		logger.error(t.getMessage(), t);
		
		if (t instanceof CertificateException) {
			throw (CertificateException)t;
		}
		
		throw new CertificateException(t);
	}
}
