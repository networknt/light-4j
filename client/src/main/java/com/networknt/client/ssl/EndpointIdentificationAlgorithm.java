package com.networknt.client.ssl;

import java.net.Socket;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import com.networknt.utility.StringUtils;

/**
 * This Enum is used to support endpoint identification algorithms LDAPS (RFC 2818), HTTPS (RFC 2818), and APIS.
 * 
 * HTTPS and LDAPS are standard algorithms, see {@link javax.net.ssl.SSLParameters#setEndpointIdentificationAlgorithm(String)}.
 * APIS is a custom algorithm for server identity check in light-4j.
 * 
 * @author Daniel Zhao
 *
 */
public enum EndpointIdentificationAlgorithm {
	HTTPS,
	LDAPS,
	APIS;
	
	/**
	 * Choose the algorithm to be used based on configuration.
	 * 
	 * @param checkIdentity - from 'verifyHostName' of the client.yml
	 * @param trustedNameSet  - from 'trustedNames' of the client.yml
	 * @return the algorithm to be used.
	 */
	public static EndpointIdentificationAlgorithm select(boolean checkIdentity, Set<String> trustedNameSet) {
		if (checkIdentity) {
			if (trustedNameSet.isEmpty()) {
				return EndpointIdentificationAlgorithm.HTTPS;
			}else {
				return EndpointIdentificationAlgorithm.APIS;
			}
		}
		
		return null;
	}
	
	/**
	 * set EndpointIdentificationAlgorithm to SSLEngine
	 * 
	 * EndpointIdentificationAlgorithm.APIS is not set because it'll cause unsupported algorithm exceptions
	 * 
	 * @param engine - ssl engine
	 * @param identityAlg - EndpointIdentificationAlgorithm to be used
	 */
	public static void setup(SSLEngine engine, EndpointIdentificationAlgorithm identityAlg) {
		if (null!=engine 
				&& null!= identityAlg
				&& EndpointIdentificationAlgorithm.APIS!=identityAlg) {
			SSLParameters parameters = engine.getSSLParameters();
			String existingAlgorithm = parameters.getEndpointIdentificationAlgorithm();
			
			if (StringUtils.isBlank(existingAlgorithm)) {
				parameters.setEndpointIdentificationAlgorithm(identityAlg.name());
				engine.setSSLParameters(parameters);
			}
		}
	}
	
	/**
	 * set EndpointIdentificationAlgorithm to SSLSocket
	 * 
	 * EndpointIdentificationAlgorithm.APIS is not set because it'll cause unsupported algorithm exceptions
	 * 
	 * @param socket - ssl socket
	 * @param identityAlg - EndpointIdentificationAlgorithm to be used
	 */
	public static void setup(Socket socket, EndpointIdentificationAlgorithm identityAlg) {
		if (null!=socket && socket.isConnected() && socket instanceof SSLSocket
				&& null!=identityAlg
				&& EndpointIdentificationAlgorithm.APIS!=identityAlg) {
			SSLSocket sslSocket = (SSLSocket)socket;
			
			SSLParameters parameters = sslSocket.getSSLParameters();
			String existingAlgorithm = parameters.getEndpointIdentificationAlgorithm();
			
			if (StringUtils.isBlank(existingAlgorithm)) {
				parameters.setEndpointIdentificationAlgorithm(identityAlg.name());
				sslSocket.setSSLParameters(parameters);
			}			
		}		
	}
}
