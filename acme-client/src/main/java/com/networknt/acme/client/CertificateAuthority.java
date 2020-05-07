package com.networknt.acme.client;

import java.io.IOException;
import java.security.KeyPair;
import java.time.Duration;
import java.time.Instant;

import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.AccountBuilder;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;

import com.networknt.acme.client.persistance.FileKeyPairStore;

public class CertificateAuthority {
	public Certificate order() throws AcmeException, InterruptedException, IOException {
		Session session = new SessionFactory().getPebbleSession();
		Account account = new AccountManager().getAccount(session);
		
		Order order = account.newOrder()
			        .domains("test.com","tested.com")
			        .create();
		for (Authorization auth : order.getAuthorizations()) {
			  if (auth.getStatus() != Status.VALID) {
			    processAuth(auth);
			  }
			}
		return null;
	}

	private void processAuth(Authorization auth) throws AcmeException, InterruptedException, IOException {
		Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);
		new HTTPChallengeResponder(challenge.getAuthorization());
		challenge.trigger();
		while (auth.getStatus() != Status.VALID) {
			  Thread.sleep(3000L);
			  auth.update();
			}
	}
	
	
}
