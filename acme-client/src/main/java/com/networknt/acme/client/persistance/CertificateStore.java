package com.networknt.acme.client.persistance;

import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Session;

public interface CertificateStore {
	Certificate retrieve(String certificateName);
	boolean store(Certificate certificate,String certificateName);
}
