package com.networknt.config;

import com.networknt.decrypt.Decryptor;

public class AESMockDecryptor implements Decryptor {

    @Override
    public String decrypt(String input) {
        return input;
    }
}
