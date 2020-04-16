package com.networknt.acme.client.persistance;

import org.shredzone.acme4j.Certificate;

public interface CertificateStore {
	Certificate retrieve(String certificateName);
	boolean store(Certificate certificate,String certificateName);
}
