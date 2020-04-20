package com.networknt.acme.client.persistance;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyPair;

public interface KeyPairStore {
	KeyPair loadOrCreateKeyPair(String name) throws  IOException;
}
