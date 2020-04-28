package com.networknt.acme.client.persistance;

import java.io.File;
import java.io.FileNotFoundException;
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
	public KeyPair getOrCreateKeyPair(String name) throws IOException {
		
		File file = new File(name);
		
		if (file.exists()) {
			return read(name, file);
		} 
		else {
			KeyPair keyPair = generateKePair();
			write(name, file, keyPair);
			return keyPair;
		}
	}
	
	private KeyPair read(String name, File accountKeyFile) throws IOException, FileNotFoundException {
		try (FileReader fr = new FileReader(accountKeyFile)) {
			logger.info("KeyPair file "+name+" exists, reading from "+name);
			return KeyPairUtils.readKeyPair(fr);
		}
	}
	
	private void write(String name, File file, KeyPair keyPair) throws IOException {
		try (FileWriter fw = new FileWriter(file)) {
			logger.info("KeyPair file "+file.getName()+" does not exists, creating the file");
			KeyPairUtils.writeKeyPair(keyPair, fw);
		}
	}
	

}
