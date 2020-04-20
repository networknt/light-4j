package com.networknt.acme.client.persistance;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;

import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class FileKeyPairStore implements KeyPairStore {
	static final Logger logger = LoggerFactory.getLogger(FileKeyPairStore.class);
	@Override
	public KeyPair loadOrCreateKeyPair(String name) throws IOException {
		
		File accountKeyFile = new File(name);
		
		if (accountKeyFile.exists()) {
			
			try (FileReader fr = new FileReader(accountKeyFile)) {
				logger.info("KeyPair file "+name+" exists, reading from "+name);
				return KeyPairUtils.readKeyPair(fr);
			}
		} 
		else {
			KeyPair accountKeyPair = KeyPairUtils.createKeyPair(2048);
			try (FileWriter fw = new FileWriter(accountKeyFile)) {
				logger.info("KeyPair file "+name+" does not exists, creating the file");
				KeyPairUtils.writeKeyPair(accountKeyPair, fw);
			}
			return accountKeyPair;
		}
	}

}
