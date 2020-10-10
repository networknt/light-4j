package com.networknt.acme.client;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.exception.AcmeException;

import com.networknt.acme.client.persistance.CertificateStore;
import com.networknt.acme.client.persistance.FileCertificateStore;
import com.networknt.acme.client.util.ACMEUtils;

public class CertificateAuthority {

	private static final String BASE_PATH = System.getProperty("user.home") + "/acme/";
	private static final String CERTIFICATE_PATH = BASE_PATH + "certificate.pem";

	public void order(String type, CertificateInstaller installer)
			throws AcmeException, InterruptedException, IOException {
		List<X509Certificate> certficateChain = getCertificate();
		if (!certficateChain.isEmpty()) {
			installer.install(certficateChain);
			return;
		}
		Session session = ACMEUtils.getSession(type);
		Certificate certifcate = new CertificateOrderer().orderCertificate(session);
		storeCertificate(certifcate);
		installer.install(certifcate.getCertificateChain());
	}

	private List<X509Certificate> getCertificate() {
		return new FileCertificateStore().retrieve(CERTIFICATE_PATH);

	}

	private void storeCertificate(Certificate certificate) {
		CertificateStore certStore = new FileCertificateStore();
		certStore.store(certificate, CERTIFICATE_PATH);
	}

}
