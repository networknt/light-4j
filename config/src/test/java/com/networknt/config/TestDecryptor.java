package com.networknt.config;

import com.networknt.decrypt.AESSaltDecryptor;

public class TestDecryptor extends AESSaltDecryptor {

    @Override
	public String decrypt(String input) {
    	return super.decrypt(input) + "-test";
    }
}
