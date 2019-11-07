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

import java.net.Socket;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Customized implementation of {@link javax.net.ssl.X509ExtendedTrustManager} to support validation of server identity using given trusted names.
 * 
 * @see javax.net.ssl.X509ExtendedTrustManager
 * @author Daniel Zhao
 *
 */
public class ClientX509ExtendedTrustManager extends X509ExtendedTrustManager implements X509TrustManager {
	private final X509TrustManager trustManager;
	private final EndpointIdentificationAlgorithm identityAlg;
	private final Set<String> trustedNameSet = new HashSet<>();
	
	public ClientX509ExtendedTrustManager(X509TrustManager trustManager, TLSConfig tlsConfig) {
		this.trustManager = Objects.requireNonNull(trustManager);
		this.identityAlg = tlsConfig.getEndpointIdentificationAlgorithm();
		this.trustedNameSet.addAll(tlsConfig.getTrustedNameSet());
	}
	
	@Override
	public X509Certificate[] getAcceptedIssuers() {
		return trustManager.getAcceptedIssuers();
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
			EndpointIdentificationAlgorithm.setup(socket, identityAlg);
			
			if (trustManager instanceof X509ExtendedTrustManager) {
				((X509ExtendedTrustManager)trustManager).checkClientTrusted(chain, authType, socket);
			}else {
				trustManager.checkClientTrusted(chain, authType);
				checkIdentity(socket, chain[0]);
			}
			
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
		try {
			EndpointIdentificationAlgorithm.setup(socket, identityAlg);
			
			if (trustManager instanceof X509ExtendedTrustManager) {
				((X509ExtendedTrustManager)trustManager).checkServerTrusted(chain, authType, socket);
			}else {
				trustManager.checkServerTrusted(chain, authType);
				checkIdentity(socket, chain[0]);
			}			
			
			doCustomServerIdentityCheck(chain[0]);
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}		
	}

	@Override
	public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
		try {
			EndpointIdentificationAlgorithm.setup(engine, identityAlg);
			
			if (trustManager instanceof X509ExtendedTrustManager) {
				((X509ExtendedTrustManager)trustManager).checkClientTrusted(chain, authType, engine);
			}else {
				trustManager.checkClientTrusted(chain, authType);
				checkIdentity(engine, chain[0]);
			}
			
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}
	}

	@Override
	public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
		try {
			EndpointIdentificationAlgorithm.setup(engine, identityAlg);
			
			if (trustManager instanceof X509ExtendedTrustManager) {
				((X509ExtendedTrustManager)trustManager).checkServerTrusted(chain, authType, engine);
			}else {
				trustManager.checkServerTrusted(chain, authType);
				checkIdentity(engine, chain[0]);
			}
			
			doCustomServerIdentityCheck(chain[0]);
		} catch (Throwable t) {
			SSLUtils.handleTrustValidationErrors(t);
		}
	}
	
	/**
	 * check server identify as per tls.trustedNames in client.yml.
	 * 
	 * Notes: this method should only be applied to verify server certificates on the client side.
	 * 
	 * @param cert X509Certificate
	 * @throws CertificateException CertificateException
	 */
	private void doCustomServerIdentityCheck(X509Certificate cert) throws CertificateException{
		if (EndpointIdentificationAlgorithm.APIS == identityAlg) {
			APINameChecker.verifyAndThrow(trustedNameSet, cert);
		}
	}

	private void checkIdentity(SSLEngine engine, X509Certificate cert)  throws CertificateException{
		if (null!=engine) {
			SSLSession session = engine.getHandshakeSession();
			checkIdentity(session, cert);
		}
	}
	
	private void checkIdentity(Socket socket, X509Certificate cert) throws CertificateException {
		if (socket != null && socket.isConnected() && socket instanceof SSLSocket) {
			SSLSocket sslSocket = (SSLSocket) socket;
			SSLSession session = sslSocket.getHandshakeSession();

			checkIdentity(session, cert);
		}
	}
	
	/**
	 * check server identify against hostnames. This method is used to enhance X509TrustManager to provide standard identity check.
	 * 
	 * This method can be applied to both clients and servers.
	 * 
	 * @param session SSLSession
	 * @param cert X509Certificate
	 * @throws CertificateException
	 */
	private void checkIdentity(SSLSession session, X509Certificate cert) throws CertificateException {
		if (session == null) {
			throw new CertificateException("No handshake session");
		}

		if (EndpointIdentificationAlgorithm.HTTPS == identityAlg) {
			String hostname = session.getPeerHost();
			APINameChecker.verifyAndThrow(hostname, cert);
		}
	}
	
	/**
	 * This method converts existing X509TrustManagers to ClientX509ExtendedTrustManagers. 
	 * 
	 * @param trustManagers array of TrustManagers
	 * @param tlsConfig TLSConfig
	 * @return TrustManager array
	 */
	public static TrustManager[] decorate(TrustManager[] trustManagers, TLSConfig tlsConfig) {
		if (null!=trustManagers && trustManagers.length>0) {
			TrustManager[] decoratedTrustManagers = new TrustManager[trustManagers.length];
			
			for (int i=0; i<trustManagers.length; ++i) {
				TrustManager trustManager = trustManagers[i];
				
				if (trustManager instanceof X509TrustManager){
					decoratedTrustManagers[i] = new ClientX509ExtendedTrustManager((X509TrustManager)trustManager, tlsConfig);
				}else {
					decoratedTrustManagers[i] = trustManager;
				}
			}
			
			return decoratedTrustManagers;
		}
		
		return trustManagers;
	}	
}
