package com.networknt.acme.client.persistance;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;

import org.shredzone.acme4j.Certificate;

public class FileCertificateStore implements CertificateStore {

	@Override
	public List<X509Certificate> retrieve(String certificatePath) {
		try {
			InputStream is = new FileInputStream(certificatePath);
			CertificateFactory cf = CertificateFactory.getInstance("X509");
			return (List<X509Certificate>) cf.generateCertificates(is);
		} catch (FileNotFoundException | CertificateException e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}

	@Override
	public boolean store(Certificate certificate, String certificateName) {
		try (FileWriter fw = new FileWriter(certificateName)) {
			certificate.writeCertificate(fw);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

}
