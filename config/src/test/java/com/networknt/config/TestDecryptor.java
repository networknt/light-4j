package com.networknt.config;

import com.networknt.decrypt.fixture.FixturePasswordDecryptor;

public class TestDecryptor extends FixturePasswordDecryptor {

    @Override
	public String decrypt(String input) {
    	return super.decrypt(input) + "-test";
    }
}
