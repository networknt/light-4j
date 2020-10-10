package com.networknt.acme.client.persistance;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.shredzone.acme4j.Certificate;

public class FileCertificateStoreTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	FileCertificateStore cut = new FileCertificateStore();
	String filename = "";

	@Before
	public void beforeClassFunction() {
		filename = folder.getRoot().getAbsolutePath() + File.pathSeparator + "certificate.pem";
	}

	@Test
	public void certificate_should_be_written_to_a_file() throws IOException {
		Certificate mockCertificate = mock(Certificate.class);
		cut.store(mockCertificate, filename);
		verify(mockCertificate).writeCertificate(any(FileWriter.class));
	}

	@Test
	public void an_empty_list_should_be_returned_if_the_certificate_is_not_stored_file_system() throws IOException {
		List<X509Certificate> certificates = cut.retrieve("notfound.pem");
		assertEquals(0, certificates.size());
	}

	@Test
	public void certificate_chain_should_be_returned_if_certificate_is_stored_in_the_file_system() throws IOException {
		List<X509Certificate> certificates = cut.retrieve("src/test/resources/data/certificate.pem");
		assertEquals(2, certificates.size());
		X509Certificate domainCertificate = certificates.get(0);
		X509Certificate rootCertificate = certificates.get(1);
		assertEquals("CN=test.com", domainCertificate.getSubjectDN().getName());
		assertEquals("CN=Pebble Intermediate CA 75159d", rootCertificate.getSubjectDN().getName());
		assertEquals(rootCertificate.getSubjectDN(), domainCertificate.getIssuerDN());
	}

}
