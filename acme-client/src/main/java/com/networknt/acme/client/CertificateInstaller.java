package com.networknt.acme.client;

import java.security.cert.X509Certificate;
import java.util.List;

public interface CertificateInstaller {
	void install(List<X509Certificate> certificateChain);
}
