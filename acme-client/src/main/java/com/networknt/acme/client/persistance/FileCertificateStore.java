package com.networknt.acme.client.persistance;

import java.io.FileWriter;
import java.io.IOException;

import org.shredzone.acme4j.Certificate;

public class FileCertificateStore implements CertificateStore {

	@Override
	public Certificate retrieve(String certificateName) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean store(Certificate certificate,String certificateName) {
		try (FileWriter fw = new FileWriter(certificateName)){
			certificate.writeCertificate(fw);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	

}
