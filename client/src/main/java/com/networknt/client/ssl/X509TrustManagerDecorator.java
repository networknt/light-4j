package com.networknt.client.ssl;

import javax.net.ssl.TrustManager;
import javax.net.ssl.X509ExtendedTrustManager;

public class X509TrustManagerDecorator {
	public static TrustManager[] decorate(TrustManager[] trustManagers, boolean checkIdentity, String trustedNames) {
		if (null!=trustManagers && trustManagers.length>0) {
			TrustManager[] decoratedTrustManagers = new TrustManager[trustManagers.length];
			
			for (int i=0; i<trustManagers.length; ++i) {
				TrustManager trustManager = trustManagers[i];
				
				if (trustManager instanceof X509ExtendedTrustManager) {
					decoratedTrustManagers[i] = new ClientX509ExtendedTrustManager((X509ExtendedTrustManager)trustManager, checkIdentity, trustedNames);
				}else {
					decoratedTrustManagers[i] = trustManager;
				}
			}
			
			return decoratedTrustManagers;
		}
		
		return trustManagers;
	}
}
