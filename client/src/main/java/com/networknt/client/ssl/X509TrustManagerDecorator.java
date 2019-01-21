package com.networknt.client.ssl;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * This class converts {@link javax.net.ssl.X509TrustManager} or {@link javax.net.ssl.X509ExtendedTrustManager} loaded by {@link java.util.ServiceLoader} to corresponding customized ones.
 * The customized trust managers support validation of server identity using given trusted names.
 * 
 * @author Daniel Zhao
 *
 */

public class X509TrustManagerDecorator {
	public static TrustManager[] decorate(TrustManager[] trustManagers) {
		if (null!=trustManagers && trustManagers.length>0) {
			TrustManager[] decoratedTrustManagers = new TrustManager[trustManagers.length];
			
			for (int i=0; i<trustManagers.length; ++i) {
				TrustManager trustManager = trustManagers[i];
				
				if (trustManager instanceof X509ExtendedTrustManager) {
					decoratedTrustManagers[i] = new ClientX509ExtendedTrustManager((X509ExtendedTrustManager)trustManager);
				}else if (trustManager instanceof X509TrustManager){
					decoratedTrustManagers[i] = new CientX509TrustManager((X509TrustManager)trustManager);
				}else {
					decoratedTrustManagers[i] = trustManager;
				}
			}
			
			return decoratedTrustManagers;
		}
		
		return trustManagers;
	}
}
