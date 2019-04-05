package com.networknt.config;

import com.networknt.decrypt.AutoAESDecryptor;

public class TestAutoDecryptor extends AutoAESDecryptor {
    @Override
    public String decrypt(String input) {
        return super.decrypt(input) + "-test";
    }
}
