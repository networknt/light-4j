package com.networknt.acme.client.http01;

import java.io.IOException;

import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.Status;
import org.shredzone.acme4j.challenge.Http01Challenge;
import org.shredzone.acme4j.exception.AcmeException;

import com.networknt.acme.client.OrderProcessor;

public class HttpOrderProcessor implements OrderProcessor {

	@Override
	public void authorizeOrder(Authorization auth) throws IOException, AcmeException, InterruptedException {
		Http01Challenge challenge = auth.findChallenge(Http01Challenge.class);
		HttpChallengeResponder responder = new HttpChallengeResponder(challenge.getAuthorization());
		responder.start();
		challenge.trigger();
		while (auth.getStatus() != Status.VALID) {
			Thread.sleep(1000L);
			auth.update();
		}
		responder.stop();
	}

}
