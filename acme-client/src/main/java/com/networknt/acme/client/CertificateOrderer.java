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
import org.shredzone.acme4j.exception.AcmeException;
import org.shredzone.acme4j.util.CSRBuilder;

import com.networknt.acme.client.persistance.FileKeyStore;
import com.networknt.acme.client.util.ACMEUtils;
import com.networknt.config.Config;

public class CertificateOrderer {
	private static ACMEConfig config = (ACMEConfig) Config.getInstance().getJsonObjectConfig("acme", ACMEConfig.class);

	private OrderProcessor orderProcessor;

	public CertificateOrderer(OrderProcessor orderProcessor) {
		this.orderProcessor = orderProcessor;
	}

	public Certificate orderCertificate() throws AcmeException, InterruptedException, IOException {
		Session session = ACMEUtils.getSession(config.getSession());
		Account account = new AccountManager().getAccount(session, AcmeClientConstants.ACCOUNT_KEY_PATH);
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
				orderProcessor.authorizeOrder(auth);
			}
		}
		return order;
	}

	private byte[] createCSR() throws IOException {
		KeyPair domainKeyPair = new FileKeyStore().getKey(AcmeClientConstants.DOMAIN_KEY_PATH);
		CSRBuilder csrb = new CSRBuilder();
		csrb.addDomain(config.getDomain());
		csrb.sign(domainKeyPair);
		csrb.write(new FileWriter(AcmeClientConstants.CERTFICATE_SIGNING_REQUEST_PATH));
		return csrb.getEncoded();
	}
}
