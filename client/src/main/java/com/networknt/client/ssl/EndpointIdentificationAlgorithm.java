package com.networknt.client.ssl;

import java.net.Socket;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;

import com.networknt.utility.StringUtils;

/**
 * This Enum is used to support endpoint identification algorithms LDAPS (RFC 2818), HTTPS (RFC 2818), and API.
 * 
 * HTTPS and LDAPS are standard algorithms, see {@link javax.net.ssl.SSLParameters#setEndpointIdentificationAlgorithm(String)}.
 * API is a custom algorithm for server identity check in light-4j.
 * 
 * @author Daniel Zhao
 *
 */
public enum EndpointIdentificationAlgorithm {
	HTTPS,
	LDAPS,
	API;
	
	/**
	 * Choose the algorithm to be used based on configuration.
	 * 
	 * @param checkIdentity
	 * @param trustedNameSet
	 * @return
	 */
	public static EndpointIdentificationAlgorithm select(boolean checkIdentity, Set<String> trustedNameSet) {
		if (checkIdentity) {
			if (trustedNameSet.isEmpty()) {
				return EndpointIdentificationAlgorithm.HTTPS;
			}else {
				return EndpointIdentificationAlgorithm.API;
			}
		}
		
		return null;
	}
	
	/**
	 * set EndpointIdentificationAlgorithm to SSLEngine
	 * 
	 * EndpointIdentificationAlgorithm.API is not set because it'll cause unsupported algorithm exceptions
	 * 
	 * @param engine
	 * @param identityAlg
	 */
	public static void setup(SSLEngine engine, EndpointIdentificationAlgorithm identityAlg) {
		if (null!=engine && EndpointIdentificationAlgorithm.HTTPS==identityAlg) {
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
	 * EndpointIdentificationAlgorithm.API is not set because it'll cause unsupported algorithm exceptions
	 * 
	 * @param socket
	 * @param identityAlg
	 */
	public static void setup(Socket socket, EndpointIdentificationAlgorithm identityAlg) {
		if (null!=socket && socket.isConnected() && socket instanceof SSLSocket
				&& EndpointIdentificationAlgorithm.HTTPS==identityAlg) {
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
