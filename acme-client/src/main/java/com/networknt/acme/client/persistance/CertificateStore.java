package com.networknt.acme.client.persistance;

import java.security.cert.X509Certificate;
import java.util.List;

import org.shredzone.acme4j.Certificate;

public interface CertificateStore {
	List<X509Certificate> retrieve(String certificatePath);

	boolean store(Certificate certificate, String certificatePath);
}
