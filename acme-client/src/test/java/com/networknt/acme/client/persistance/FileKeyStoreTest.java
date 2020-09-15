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
import org.wildfly.common.Assert;

public class FileKeyStoreTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();
	FileKeyStore cut = new FileKeyStore();
	String filename = "";

	@Before
	public void beforeClassFunction() {
		filename = folder.getRoot().getAbsolutePath() + File.pathSeparator + "account.key";
	}

	@Test
	public void key_pair_should_be_generated_and_stored_if_account_key_file_doesnt_exist() throws IOException {

		KeyPair key = cut.getKey(filename);
		assertNotNull(key.getPrivate());
		assertNotNull(key.getPublic());
		Assert.assertTrue(new File(filename).exists());

	}

	@Test
	public void key_pair_should_be_read_from_file_if_account_key_file_doesnt_exist() throws IOException {

		KeyPair generated = cut.getKey(filename);
		KeyPair saved = cut.getKey(filename);
		assertEquals(generated.getPrivate(), saved.getPrivate());
		assertEquals(generated.getPublic(), saved.getPublic());
	}

}
