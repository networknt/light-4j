package com.networknt.config;

import com.networknt.decrypt.AutoAESSaltDecryptor;

/** Test-only customization using the password supplied by the test case. */
public class TestDecryptor extends AutoAESSaltDecryptor {
    @Override
    public String decrypt(String input) {
        return super.decrypt(input) + "-test";
    }
}
