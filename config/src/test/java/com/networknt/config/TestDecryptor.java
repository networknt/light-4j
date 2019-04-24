package com.networknt.config;

import com.networknt.decrypt.AESDecryptor;

public class TestDecryptor extends AESDecryptor {

    @Override
	public String decrypt(String input) {
    	return super.decrypt(input) + "-test";
    }
}
