package com.networknt.config;

import com.networknt.decrypt.AESSaltDecryptor;

public class TestDecryptor extends AESSaltDecryptor {
    // Custom-provider fixture: the test runner supplies the password explicitly.
    @Override
    protected char[] getPassword() {
        String password = System.getProperty("light_4j_config_password");
        if (password == null || password.isBlank()) {
            throw new IllegalStateException("An explicit fixture password is required.");
        }
        return ownedPassword(password.toCharArray());
    }

    @Override
	public String decrypt(String input) {
    	return super.decrypt(input) + "-test";
    }
}
