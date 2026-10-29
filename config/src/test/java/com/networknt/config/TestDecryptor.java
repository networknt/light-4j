package com.networknt.config;

import com.networknt.decrypt.AESSaltDecryptor;

public class TestDecryptor extends AESSaltDecryptor {
    // Explicit fixture secret; the base class intentionally has no public fallback.
    @Override
    protected char[] getPassword() { return ownedPassword("light".toCharArray()); }


    @Override
	public String decrypt(String input) {
    	return super.decrypt(input) + "-test";
    }
}
