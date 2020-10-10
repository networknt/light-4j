package com.networknt.acme.client;

import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;

import org.shredzone.acme4j.Account;
import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Certificate;
import org.shredzone.acme4j.Order;
import org.shredzone.acme4j.Session;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;

import com.networknt.acme.client.persistance.FileKeyStore;
import com.networknt.config.Config;

public class CertificateOrderer {
	private static final String BASE_PATH = System.getProperty("user.home") + "/acme/";
	private static final String CERTFICATE_SIGNING_REQUEST_PATH = BASE_PATH + "domain.csr";
	private static final String ACCOUNT_KEY_PATH = BASE_PATH + "account.key";
	private static final String DOMAIN_KEY_PATH = BASE_PATH + "domain.key";
	private static ACMEConfig config = (ACMEConfig) Config.getInstance().getJsonObjectConfig("acme", ACMEConfig.class);

	public Certificate orderCertificate(Session session) throws AcmeException, InterruptedException, IOException {
		Account account = new AccountManager().getAccount(session, ACCOUNT_KEY_PATH);
		Order order = createOrder(account);
		byte[] csr = createCSR();
		order.execute(csr);
		while (order.getStatus() != Status.VALID) {
			Thread.sleep(3000L);
			order.update();
		}
		return order.getCertificate();
	}

	private Order createOrder(Account account) throws AcmeException, InterruptedException, IOException {
		String domain = config.getDomain();
		Order order = account.newOrder().domains(domain).create();
		for (Authorization auth : order.getAuthorizations()) {
			if (auth.getStatus() != Status.VALID) {
				processAuth(auth);
			}
		}
		return order;
	}

	private void processAuth(Authorization auth) throws AcmeException, InterruptedException, IOException {
		Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);
		HTTPChallengeResponder responder = new HTTPChallengeResponder(challenge.getAuthorization());
		responder.start();
		challenge.trigger();
		while (auth.getStatus() != Status.VALID) {
			Thread.sleep(1000L);
			auth.update();
		}
		responder.stop();
	}

	private byte[] createCSR() throws IOException {
		KeyPair domainKeyPair = new FileKeyStore().getKey(DOMAIN_KEY_PATH);
		CSRBuilder csrb = new CSRBuilder();
		csrb.addDomain("test.com");
		csrb.sign(domainKeyPair);
		csrb.write(new FileWriter(CERTFICATE_SIGNING_REQUEST_PATH));
		return csrb.getEncoded();
	}
}
