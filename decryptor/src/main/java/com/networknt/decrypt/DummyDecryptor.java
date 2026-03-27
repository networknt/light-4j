package com.networknt.decrypt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A dummy implementation of Decryptor that returns the input string as is,
 * provided it has the correct prefix.
 */
public class DummyDecryptor implements Decryptor {
    private static final Logger logger = LoggerFactory.getLogger(DummyDecryptor.class);
    /**
     * Default constructor for DummyDecryptor.
     */
    public DummyDecryptor() {
        if(logger.isInfoEnabled()) logger.info("DummyDecryptor is constructed.");
    }

    @Override
    public String decrypt(String input) {
        if (!input.startsWith(CRYPT_PREFIX)) {
            logger.error("The secret text is not started with prefix " + CRYPT_PREFIX);
            throw new RuntimeException("Unable to decrypt, input string does not start with 'CRYPT'.");
        }
        String[] parts = input.split(":");
        if(parts.length != 2) {
            logger.error("The secret text is not formatted correctly with CRYPT:text");
            throw new RuntimeException("Unable to decrypt, input string is not formatted correctly with CRYPT:text");
        }
        return parts[1];
    }
}
