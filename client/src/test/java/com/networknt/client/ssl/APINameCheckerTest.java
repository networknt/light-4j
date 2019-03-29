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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.security.auth.x500.X500Principal;

import org.junit.Test;
import org.mockito.Mockito;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class APINameCheckerTest {
	private static final String x500DistinguishedName = "CN=service.com, OU=Unit Test, O=Test, C=CA";
	private static final String x500DistinguishedNameWithWildcard = "CN=*.service.com, OU=Unit Test, O=Test, C=CA";
	private static final Collection<List<?>> subjectAlternativeNames = new ArrayList<>();
	
	static {
		// see X509Certificate.getSubjectAlternativeNames()
		List entry = new ArrayList();
		
		entry.add(2);// DNS
		entry.add("*.service.com");
		
		subjectAlternativeNames.add(entry);
		
		entry = new ArrayList();
		entry.add(2);// DNS
		entry.add("my.prod.service.com");	
		
		subjectAlternativeNames.add(entry);
	}

	@Test
	public void cn_is_matched_when_subject_alternative_names_are_not_set() {
		String name="service.com";
		
		X509Certificate cert = Mockito.mock(X509Certificate.class);
		X500Principal principal = new X500Principal(x500DistinguishedName);
		
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
		
		assertTrue(APINameChecker.verify(name, cert));
	}
	
	@Test
	public void wildcard_in_cn_is_accepted() {
		String name="my.service.com";
		
		X509Certificate cert = Mockito.mock(X509Certificate.class);
		X500Principal principal = new X500Principal(x500DistinguishedNameWithWildcard);
		
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
		
		assertTrue(APINameChecker.verify(name, cert));		
	}
	
	@Test
	public void subject_alternative_names_are_accepted() throws CertificateParsingException {
		Set<String> validNames = new HashSet<>(Arrays.asList("my.service.com", "123.service.com", "www.service.com", "prod.service.com", "my.prod.service.com"));
		Set<String> invalidNames = new HashSet<>(Arrays.asList("service.com", "123.prod.service.com", "www.prod.service.com"));
		
		X509Certificate cert = Mockito.mock(X509Certificate.class);
		
		Mockito.when(cert.getSubjectAlternativeNames()).thenReturn(subjectAlternativeNames);
	
		
		for (String name: validNames) {
			assertTrue(APINameChecker.verify(name, cert));	
		}
		
		assertFalse(APINameChecker.verify(invalidNames, cert));	
	}
	
	@Test
	public void cn_is_not_used_if_subject_alternative_names_are_set() throws CertificateParsingException {
		String name="service.com";
		
		X509Certificate cert = Mockito.mock(X509Certificate.class);
		
		X500Principal principal = new X500Principal(x500DistinguishedName);
		Mockito.when(cert.getSubjectX500Principal()).thenReturn(principal);
		
		Mockito.when(cert.getSubjectAlternativeNames()).thenReturn(subjectAlternativeNames);
		
		assertFalse(APINameChecker.verify(name, cert));
	}
	
	@Test(expected=java.security.cert.CertificateException.class)
	public void exception_is_thrown_if_not_match() throws CertificateException {
		Set<String> invalidNames = new HashSet<>(Arrays.asList("service.com", "123.prod.service.com", "www.prod.service.com"));
		
		X509Certificate cert = Mockito.mock(X509Certificate.class);
		
		Mockito.when(cert.getSubjectAlternativeNames()).thenReturn(subjectAlternativeNames);
	
		APINameChecker.verifyAndThrow(invalidNames, cert);
	}
}
