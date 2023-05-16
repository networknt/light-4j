package com.networknt.config;

import com.networknt.decrypt.AutoAESSaltDecryptor;

public class TestAutoDecryptor extends AutoAESSaltDecryptor {
    @Override
    public String decrypt(String input) {
        return super.decrypt(input) + "-test";
    }
}
