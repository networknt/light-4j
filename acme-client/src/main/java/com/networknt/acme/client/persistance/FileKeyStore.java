package com.networknt.acme.client.persistance;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;

import org.shredzone.acme4j.util.KeyPairUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileKeyStore implements KeyStore {
	private static final Logger logger = LoggerFactory.getLogger(FileKeyStore.class);

	@Override
	public KeyPair getKey(String name) {
		File file = new File(name);
		if (file.exists()) {
			return readKeyFromFile(file);
		} else {
			return createKey(file);
		}
	}

	private KeyPair readKeyFromFile(File file) {
		String fileName = file.getName();
		try (FileReader fr = new FileReader(file)) {
			logger.info("KeyPair file " + fileName + " exists, reading from " + fileName);
			return KeyPairUtils.readKeyPair(fr);
		} catch (IOException e) {
			logger.error("Error while reading file " + fileName, e);
			return createKey(file);
		}
	}

	private void writeKeyToFile(File file, KeyPair keyPair) {
		String fileName = file.getName();
		try (FileWriter fw = new FileWriter(file)) {
			logger.info("KeyPair file " + fileName + " does not exists, creating the file");
			KeyPairUtils.writeKeyPair(keyPair, fw);
		} catch (IOException e) {
			logger.error("Error while writing to file " + fileName, e);
		}
	}

	private KeyPair createKey(File file) {
		KeyPair keyPair = generateKePair();
		writeKeyToFile(file, keyPair);
		return keyPair;
	}
}
