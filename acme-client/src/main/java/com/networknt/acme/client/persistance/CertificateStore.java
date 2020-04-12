package com.networknt.acme.client.persistance;

import java.security.cert.X509Certificate;

public interface CertificateStore {
	X509Certificate retrieve(String certificateName);
	boolean store(String certificateName);
}
