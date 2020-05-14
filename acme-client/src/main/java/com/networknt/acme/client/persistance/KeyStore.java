package com.networknt.acme.client.persistance;

import java.io.IOException;
import java.security.KeyPair;

import org.shredzone.acme4j.util.KeyPairUtils;

public interface KeyStore {
	KeyPair getKey(String name) throws  IOException;
	default KeyPair generateKePair() {
		return KeyPairUtils.createKeyPair(2048);
	}
}
