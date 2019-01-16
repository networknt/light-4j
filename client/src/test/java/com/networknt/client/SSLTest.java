package com.networknt.client;

import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Provider.Service;
import java.security.Security;

import org.junit.Test;

public class SSLTest {

	@Test
	public void testProviders() throws NoSuchAlgorithmException {
		//Provider[] p = Security.getProviders();
		
		Provider[] p = Security.getProviders("SSLContext.TLS");
		
		
		for (int i=0; i<p.length; ++i) {
			System.out.println(p[i].getName());
			System.out.println("---"+p[i].keySet());
			
			Service s = p[i].getService("SSLContext", "TLS");
			
			if (null!=s) {
				Object im= s.newInstance((Object)null);
				
				System.out.println();
			}
		}
		
		
		
		Object o = Security.getProvider("SSLContext");
		
		//Object o = ServiceLoader.load(SSLContextSpi.class);
		
		
		System.out.println();
	}
	
	
}
