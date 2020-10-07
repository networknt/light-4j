package com.networknt.acme.client.persistance;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.security.KeyPair;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.shredzone.acme4j.Certificate;
import org.wildfly.common.Assert;

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
	public void key_pair_should_be_generated_and_stored_if_account_key_file_doesnt_exist() throws IOException {
		Certificate certificate  = new Certificate(null, null);
		KeyPair key = cut.store(certificate, certificateName)
		assertNotNull(key.getPrivate());
		assertNotNull(key.getPublic());
		Assert.assertTrue(new File(filename).exists());

	}

}
