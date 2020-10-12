package com.networknt.acme.client;

import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.exception.AcmeException;

import com.networknt.acme.client.http01.HttpOrderProcessor;
import com.networknt.acme.client.persistance.CertificateStore;
import com.networknt.acme.client.persistance.FileCertificateStore;

public class CertificateAuthority {

	public void order(CertificateInstaller installer) throws AcmeException, InterruptedException, IOException {
		List<X509Certificate> certficateChain = getCertificate();
		if (!certficateChain.isEmpty()) {
			installer.install(certficateChain);
			return;
		}
		Certificate certifcate = new CertificateOrderer(new HttpOrderProcessor()).orderCertificate();
		storeCertificate(certifcate);
		installer.install(certifcate.getCertificateChain());
	}

	private List<X509Certificate> getCertificate() {
		return getCertificateStore().retrieve(AcmeClientConstants.CERTIFICATE_PATH);

	}

	private void storeCertificate(Certificate certificate) {
		CertificateStore certStore = getCertificateStore();
		certStore.store(certificate, AcmeClientConstants.CERTIFICATE_PATH);
	}

	private CertificateStore getCertificateStore() {
		return new FileCertificateStore();
	}

}
