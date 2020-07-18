package com.networknt.acme.client;

import java.security.KeyPair;

import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.exception.AcmeException;

import com.networknt.acme.client.persistance.FileKeyStore;

public class AccountManager {
	private static final String BASE_PATH = "user.home";

	public Account getAccount(Session session) throws  AcmeException {
		KeyPair  accountKey = null;
		Account account = null;
		accountKey = new FileKeyStore().getKey(System.getProperty(BASE_PATH)+"/account.key");
		account = new AccountBuilder()
				.agreeToTermsOfService()
				.useKeyPair(accountKey)
				.create(session);

		return account;
	}
}
