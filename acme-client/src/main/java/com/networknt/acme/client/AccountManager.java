package com.networknt.acme.client;

import java.io.IOException;
import java.security.KeyPair;

import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.exception.AcmeException;

import com.networknt.acme.client.persistance.FileKeyPairStore;

public class AccountManager {
	public Account getAccount(Session session) {
		KeyPair  accountKey = null;
		Account account = null;
		try {
			accountKey = new FileKeyPairStore().getOrCreateKeyPair(System.getProperty("user.home")+"/account.key");
			 
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			 account = new AccountBuilder()
			        .agreeToTermsOfService()
			        .useKeyPair(accountKey)
			        .create(session);
		} catch (AcmeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return account;
	}
}
