package com.networknt.acme.client;

import java.io.IOException;

import org.shredzone.acme4j.Authorization;
import org.shredzone.acme4j.exception.AcmeException;

public interface OrderProcessor {
	void authorizeOrder(Authorization auth) throws IOException, AcmeException, InterruptedException;
}
